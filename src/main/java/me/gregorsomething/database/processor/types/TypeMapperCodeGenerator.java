package me.gregorsomething.database.processor.types;

import com.squareup.javapoet.CodeBlock;
import lombok.RequiredArgsConstructor;
import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.processor.RepositoryProcessor;
import me.gregorsomething.database.processor.helpers.Pair;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RequiredArgsConstructor
public class TypeMapperCodeGenerator {
    private final RepositoryProcessor processor;
    private final TypeMapperResolver typeMapperResolver;

    public CodeBlock forResultSet(ExecutableElement element, Query query) {
        return CodeBlock.builder()
                .addStatement("return this.database.query($S$L)", query.value(), this.databaseQueryArgsFor(element))
                .build();
    }

    public CodeBlock forOptional(ExecutableElement element, Query query, TypeMirror optionalType) {
        CodeBlock.Builder code = CodeBlock.builder();
        code.beginControlFlow("try ($T rs = this.database.query($S$L))",
                ResultSet.class, query.value(), this.databaseQueryArgsFor(element));
        this.insetNoRowsCheck(code, query, false, false);
        code.addStatement("rs.next()");

        // External mapper
        if (this.typeMapperResolver.hasTypeDefFromResultSet(optionalType)) {
            Pair<TypeMirror, String> mapper = this.typeMapperResolver.getTypeMapperFromResultSet(optionalType);
            code.addStatement("return $T.ofNullable(T$.$L(rs, 1))", Optional.class, mapper.left(), mapper.right());
            return endTryBlockAndAddCatchIfNeeded(element, code);
        }

        // Builtin mapper
        Pair<String, String> mapper = this.typeMapperResolver.getBuiltinMapperForType(optionalType);

        // If needs null check or to avoid null pointer when dealing with calling other method
        code.addStatement("var tmp = rs.$L(1)", mapper.left());
        String ofMethodName = "ofNullable"; // For Optional.of..., when null check is done by db, will not need other
        if (this.typeMapperResolver.needsNullCheck(optionalType)) {
            code
                    .beginControlFlow("if (rs.wasNull())")
                    .addStatement("return $T.empty()", Optional.class)
                    .endControlFlow();
            ofMethodName = "of";
        }
        if (mapper.right() != null) {
            code.addStatement("return $T.$L(tmp).map(v -> v.$L)", Optional.class, ofMethodName, mapper.right());
        } else {
            code.addStatement("return $T.$L(tmp)", Optional.class, ofMethodName);
        }

        return endTryBlockAndAddCatchIfNeeded(element, code);
    }

    public CodeBlock forList(ExecutableElement element, Query query, TypeMirror listElementType) {
        CodeBlock.Builder code = CodeBlock.builder();
        code.beginControlFlow("try ($T rs = this.database.query($S$L))",
                ResultSet.class, query.value(), this.databaseQueryArgsFor(element));
        this.insetNoRowsCheck(code, query, true, false);
        code.addStatement("$T<$T> list = new $T<>();", List.class, listElementType, ArrayList.class);
        code.beginControlFlow("while (rs.next())");

        // External mapper
        if (this.typeMapperResolver.hasTypeDefFromResultSet(listElementType)) {
            Pair<TypeMirror, String> mapper = this.typeMapperResolver.getTypeMapperFromResultSet(listElementType);
            code.addStatement("list.add($T.$L(rs, 1))", mapper.left(), mapper.right());
            return endTryBlockAndAddCatchIfNeeded(element, code);

        }

        // Builtin mapper
        Pair<String, String> mapper = this.typeMapperResolver.getBuiltinMapperForType(listElementType);
        boolean needsNullCheck = this.typeMapperResolver.needsNullCheck(listElementType);

        // No need for null check, and null pointer cant occure here
        if (!needsNullCheck && mapper.right() == null) {
            code
                    .addStatement("list.add(rs.$L(1))", mapper.left())
                    .endControlFlow() // While
                    .addStatement("return list");
            return endTryBlockAndAddCatchIfNeeded(element, code); // try
        }

        // If need null check or to avoid null pointer when dealing with calling other method
        code.addStatement("var tmp = rs.$L(1)", mapper.left());
        if (needsNullCheck) {
            code
                    .beginControlFlow("if (rs.wasNull())")
                    .addStatement("list.add($L)", query.defaultValue())
                    .addStatement("continue")
                    .endControlFlow();
        }
        // Additional method call to get value to needed type
        if (mapper.right() != null) {
            code.addStatement("list.add(tmp == null ? null : tmp.$L)", mapper.right());
        } else {
            code.addStatement("list.add(tmp)");
        }


        code.endControlFlow() // while
                .addStatement("return list");
        return endTryBlockAndAddCatchIfNeeded(element, code); //try
    }

    public CodeBlock forSimpleType(ExecutableElement element, Query query) {
        CodeBlock.Builder code = CodeBlock.builder();
        code.beginControlFlow("try ($T rs = this.database.query($S$L))",
                ResultSet.class, query.value(), this.databaseQueryArgsFor(element));
        this.insetNoRowsCheck(code, query, false, false);
        code.addStatement("rs.next()");

        // External mapper
        if (this.typeMapperResolver.hasTypeDefFromResultSet(element.getReturnType())) {
            Pair<TypeMirror, String> mapper = this.typeMapperResolver.getTypeMapperFromResultSet(element.getReturnType());
            code.addStatement("return $T.$L(rs, 1)", mapper.left(), mapper.right());
            return endTryBlockAndAddCatchIfNeeded(element, code);
        }

        // Builtin mapper
        Pair<String, String> mapper = this.typeMapperResolver.getBuiltinMapperForType(element.getReturnType());
        boolean needsNullCheck = this.typeMapperResolver.needsNullCheck(element.getReturnType());

        // No need for null check, and null pointer cant occure here
        if (!needsNullCheck && mapper.right() == null) {
            code.addStatement("return rs.$L(1)", mapper.left());
            return endTryBlockAndAddCatchIfNeeded(element, code);
        }

        // If need null check or to avoid null pointer when dealing with calling other method
        code.addStatement("var tmp = rs.$L(1)", mapper.left());
        if (needsNullCheck) {
            code
                    .beginControlFlow("if (rs.wasNull())")
                    .addStatement("return $L", query.defaultValue())
                    .endControlFlow();
        }
        // Additional method call to get value to needed type
        if (mapper.right() != null) {
            code.addStatement("return tmp == null ? null : tmp.$L", mapper.right());
        } else {
            code.addStatement("return tmp");
        }
        return endTryBlockAndAddCatchIfNeeded(element, code);
    }

    private String databaseQueryArgsFor(ExecutableElement element) {
        StringBuilder args = new StringBuilder();
        for (VariableElement parameter : element.getParameters()) {
            args.append(", ").append(parameter.getSimpleName().toString());
        }
        return args.toString();
    }

    private void insetNoRowsCheck(CodeBlock.Builder code, Query query, boolean isList, boolean isOptional) {
        code.beginControlFlow("if (!rs.isBeforeFirst())");
        if (query.onNoResultThrow()) {
            code.addStatement("throw new $T()", NoSuchElementException.class);
        } else if (isList) {
            code.addStatement("return $T.of()", List.class);
        } else if (isOptional) {
            code.addStatement("return $T.empty()", Optional.class);
        } else {
            code.addStatement("return $L", query.defaultValue());
        }
        code.endControlFlow();
    }

    private CodeBlock endTryBlockAndAddCatchIfNeeded(ExecutableElement element, CodeBlock.Builder code) {
        if (element.getThrownTypes().isEmpty()) {
            code
                    .nextControlFlow("catch ($T e)", SQLException.class)
                    .addStatement("throw new $T(e)", RuntimeException.class);
        }
        return code.endControlFlow().build();
    }
}
