package me.gregorsomething.database.processor.types;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import lombok.RequiredArgsConstructor;
import me.gregorsomething.database.processor.RepositoryProcessor;
import me.gregorsomething.database.processor.helpers.Pair;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ComplexTypeMapperGenerator {

    private final RepositoryProcessor processor;
    private final TypeMapperResolver typeMapperResolver;

    /**
     * Makes new mapper method on repository class
     */
    public void generateMapperMethodForTypeWithConstructor(TypeMirror returnType, List<Pair<Integer, String>> selectedItems,
                                                           String methodName, ExecutableElement constructor) {
        MethodSpec.Builder method = this.generateMethodHeadFor(methodName, returnType);
        CodeBlock.Builder code = CodeBlock.builder();
        if (returnType instanceof DeclaredType t) {
            System.out.println("Tere");
            TypeElement element = (TypeElement) t.asElement();
            List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
            System.out.println(element);
        }
        this.readParametersToVariables(selectedItems, constructor.getParameters(), returnType, code);
        this.addConstructorCallAndReturn(code, returnType, constructor);
        method.addCode(code.build());
        this.typeMapperResolver.getCurrentRepoBuilder().addMethod(method.build());
    }

    public void generateMapperMethodForTypeWithStaticMethod(TypeMirror returnType, List<Pair<Integer, String>> selectedItems,
                                                           String methodName, ExecutableElement staticMethod) {
        MethodSpec.Builder method = this.generateMethodHeadFor(methodName, returnType);
        CodeBlock.Builder code = CodeBlock.builder();
        this.readParametersToVariables(selectedItems, staticMethod.getParameters(), returnType, code);
        this.addStaticMethodCallAndReturn(code, returnType, staticMethod);
        method.addCode(code.build());
        this.typeMapperResolver.getCurrentRepoBuilder().addMethod(method.build());
    }

    private MethodSpec.Builder generateMethodHeadFor(String methodName, TypeMirror returnType) {
        return MethodSpec.methodBuilder(methodName)
                .returns(TypeName.get(returnType))
                .addException(SQLException.class)
                .addParameter(ParameterSpec
                        .builder(ResultSet.class, "rs")
                        .build()
                ).addModifiers(Modifier.PRIVATE);
    }

    private void readParametersToVariables(List<Pair<Integer, String>> selectedItems, List<? extends VariableElement> variableElements,
                                           TypeMirror returnType , CodeBlock.Builder code) {
        Map<TypeMirror, TypeMirror> typeMap = this.makeParamTypeMap(returnType);
        for (VariableElement element : variableElements) {
            Pair<Integer, String> item = selectedItems.stream()
                    .filter(i -> i.right().equalsIgnoreCase(element.getSimpleName().toString()))
                    .findFirst()
                    .orElseThrow();
            this.readParameterVariable(code, item.left(),
                    element.getSimpleName().toString(),
                    typeMap.getOrDefault(element.asType(), element.asType()));
        }
    }

    private Map<TypeMirror, TypeMirror> makeParamTypeMap(TypeMirror returnType) {
        if (returnType instanceof DeclaredType type
                && type.asElement() instanceof TypeElement element) {
            Map<TypeMirror, TypeMirror> typesMapping = new HashMap<>();
            List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
            List<? extends TypeMirror> typeArguments = type.getTypeArguments();
            if (typeParameters.size() != typeArguments.size())
                throw new IllegalStateException("Type parameters and type arguments should be same size");
            for (int i = 0; i < typeParameters.size(); i++) {
                TypeMirror genericType = typeParameters.get(i).asType();
                TypeMirror actualType = typeArguments.get(i);
                typesMapping.put(genericType, actualType);
            }
            return typesMapping;
        }
        return Map.of();
    }

    private void readParameterVariable(CodeBlock.Builder code, int resultSetPos, String variableName, TypeMirror type) {
        if (this.typeMapperResolver.hasTypeDefFromResultSet(type)) {
            Pair<TypeMirror, String> mapper = this.typeMapperResolver.getTypeMapperFromResultSet(type);
            code.addStatement("$T $L = T$.$L(rs, $L)", TypeName.get(type), variableName, mapper.left(), mapper.right(), resultSetPos);
        } else {
            Pair<String, String> mapper = this.typeMapperResolver.getBuiltinMapperForType(type);
            if (mapper.right() != null) {
                code.addStatement("var $LTmp = rs.$L($L)", variableName, mapper.left(), resultSetPos)
                        .addStatement("$T $L = $LTmp == null ? null : $LTmp.$L", TypeName.get(type), variableName, variableName, variableName, mapper.right());
            } else {
                code.addStatement("$T $L = rs.$L($L)", TypeName.get(type), variableName, mapper.left(), resultSetPos);
                if (!type.getKind().isPrimitive() && this.typeMapperResolver.needsNullCheck(type)) {
                    code.beginControlFlow("if (rs.wasNull())")
                            .addStatement("$L = null", variableName)
                            .endControlFlow();
                }
            }
        }
    }

    private void addConstructorCallAndReturn(CodeBlock.Builder code, TypeMirror returnType, ExecutableElement constructor) {
        String constructorArgs = constructor.getParameters()
                .stream()
                .map(v -> v.getSimpleName().toString())
                .collect(Collectors.joining(", "));
        code.addStatement("return new $T($L)", TypeName.get(returnType), constructorArgs);
    }

    private void addStaticMethodCallAndReturn(CodeBlock.Builder code, TypeMirror returnType, ExecutableElement method) {
        String methodArgs = method.getParameters()
                .stream()
                .map(v -> v.getSimpleName().toString())
                .collect(Collectors.joining(", "));
        code.addStatement("return $T.$L($L)", TypeName.get(returnType), method.getSimpleName().toString(), methodArgs);
    }
}
