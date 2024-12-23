package me.gregorsomething.database.processor;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.processor.helpers.ElementUtils;
import me.gregorsomething.database.processor.helpers.SqlHelper;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class QuerySubProcessor {
    private final RepositoryProcessor processor;
    private final SqlHelper helper;

    public MethodSpec createQueryMethod(ExecutableElement element) {
        Query query = this.validateQueryAnnotationOn(element);
        MethodSpec.Builder builder = ElementUtils.overrideMethod(element, Modifier.PUBLIC);

        if (element.getThrownTypes().size() > 1)
            throw new ProcessingValidationException("Only one exception can be thrown", element);
        if (element.getThrownTypes().size() == 1 && !this.processor.isOfType(element.getThrownTypes().getFirst(), SQLException.class))
            throw new ProcessingValidationException("Only SQLException can be thrown", element);
        if (element.getThrownTypes().size() == 1)
            builder.addException(SQLException.class);
        else
            builder.addAnnotation(SneakyThrows.class);

        builder.addCode(this.generateCodeFor(element, query));

        return builder.build();
    }

    private Query validateQueryAnnotationOn(ExecutableElement element) {
        if (element.getReturnType().getKind().equals(TypeKind.VOID))
            throw new ProcessingValidationException("Statement cant have return type of void!", element);
        Query[] annotations = element.getAnnotationsByType(Query.class);
        if (annotations.length != 1)
            throw new ProcessingValidationException("Statement annotation can be use once on an element!", element);

        return annotations[0];
    }

    private CodeBlock generateCodeFor(ExecutableElement element, Query query) {
        TypeMirror type = element.getReturnType();
        if (element.getReturnType().getKind().isPrimitive()) {
            return this.generateCodeNoOptionalForMapper(element, query, this.helper.simpleMapperFor(type, 1));
        }
        if (this.processor.isOfType(type, ResultSet.class)) {
            return generateForResultSet(element, query);
        }
        if (this.processor.isBaseTypeOf(type, Optional.class)) {
            return this.generateForOptionalSimple(element, query);
        }
        if (this.processor.isBaseTypeOf(type, List.class)) {
            return this.generatorForListSimple(element, query);
        }
        return this.generateCodeNoOptionalForMapper(element, query, this.helper.simpleMapperFor(type, 1));
    }

    private CodeBlock generateForResultSet(ExecutableElement element, Query query) {
        CodeBlock.Builder builder = CodeBlock.builder();
        StringBuilder args = new StringBuilder();
        for (VariableElement parameter : element.getParameters()) {
            args.append(", ").append(parameter.getSimpleName().toString());
        }
        builder.addStatement("return this.database.query($S$L)", query.value(), args.toString());
        return builder.build();
    }

    private CodeBlock generateForOptionalSimple(ExecutableElement element, Query query) {
        return generateCodeWithMappingUsingMethodParmType(element, query, "queryAndMap");
    }

    private CodeBlock generatorForListSimple(ExecutableElement element, Query query) {
        return generateCodeWithMappingUsingMethodParmType(element, query, "queryAndMapAll");
    }

    private CodeBlock generateCodeWithMappingUsingMethodParmType(ExecutableElement element, Query query, String method) {
        TypeMirror type = this.getTypeParameterOf(element.getReturnType(), element);
        StringBuilder args = new StringBuilder();
        for (VariableElement parameter : element.getParameters()) {
            args.append(", ").append(parameter.getSimpleName().toString());
        }
        String mapper = query.mapping().isEmpty() ? this.getAutoMappingFor(type, 1) : query.mapping();
        if (mapper == null)
            throw new ProcessingValidationException("Did not find auto mapping to paramater return type: " + TypeName.get(type).toString(), element);
        return CodeBlock.builder()
                .addStatement("return this.database.$L($S, $L$L)", method, query.value(), mapper, args)
                .build();
    }

    private CodeBlock generateCodeNoOptionalForMapper(ExecutableElement element, Query query, String mapper) {
        if (element.getReturnType().getKind().isPrimitive() && query.defaultValue().isEmpty())
            throw new ProcessingValidationException("Primitive types must use default value type.", element);
        if ((mapper == null || mapper.isEmpty()) && query.mapping().isEmpty())
            throw new ProcessingValidationException("Did not find auto mapping.", element);
        mapper = query.mapping().isEmpty() ? mapper : query.mapping();

        StringBuilder args = new StringBuilder();
        for (VariableElement parameter : element.getParameters()) {
            args.append(", ").append(parameter.getSimpleName().toString());
        }
        return CodeBlock.builder()
                .beginControlFlow("try ($T rs = this.database.query($S$L))", ResultSet.class, query.value(), args)
                .beginControlFlow("if (!rs.isBeforeFirst())")
                .addStatement("return $L", query.defaultValue().isEmpty() ? "null" : query.defaultValue())
                .endControlFlow()
                .addStatement("rs.next()")
                .addStatement("return $L", mapper)
                .endControlFlow()
                .build();
    }

    private TypeMirror getTypeParameterOf(TypeMirror typeMirror, ExecutableElement element) {
        if (typeMirror instanceof DeclaredType type) {
            List<? extends TypeMirror> types = type.getTypeArguments();
            if (types.size() != 1)
                throw new ProcessingValidationException("No parameter type was specified for DeclaredType or too may were!", element);
            return types.getFirst();
        }
        throw new ProcessingValidationException("Expected DeclaredType element!", element);
    }

    private String getAutoMappingFor(TypeMirror type, int index) {
        String mapping = this.helper.simpleMapperFor(type, index);
        if (mapping == null)
            return null;
        return "rs -> " + mapping;
    }
}
