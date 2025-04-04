package me.gregorsomething.database.processor.helpers;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import me.gregorsomething.database.processor.ProcessingValidationException;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ElementUtils {

    private ElementUtils() {}

    public static MethodSpec.Builder overrideMethod(ExecutableElement element, Modifier... modifiers) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(element.getSimpleName().toString())
                .addAnnotation(Override.class)
                .addModifiers(modifiers)
                .returns(TypeName.get(element.getReturnType()));
        List<? extends VariableElement> parameters = element.getParameters();
        for (VariableElement parameter : parameters) {
            builder.addParameter(TypeName.get(parameter.asType()), parameter.getSimpleName().toString());
        }
        return builder;
    }

    /**
     * Gets declared types inner type parameter, e.g. Optional<Long> will output Long
     * @param typeMirror Type like Optional List or something with extra type
     * @param element element on what that type is, used for errors
     * @return Type parameter/argument
     */
    public static TypeMirror getTypeParameterOf(TypeMirror typeMirror, ExecutableElement element) {
        if (typeMirror instanceof DeclaredType type) {
            List<? extends TypeMirror> types = type.getTypeArguments();
            if (types.size() != 1)
                throw new ProcessingValidationException("No parameter type was specified for DeclaredType or too may were!", element);
            return types.getFirst();
        }
        throw new ProcessingValidationException("Expected DeclaredType element!", element);
    }

    /**
     * Maps type generics to their actual types, where the key is a generic {@link javax.lang.model.type.TypeMirror}
     * (e.g., {@code TypeMirror{type='T'}}), and the value is its corresponding resolved type from {@code returnType}.
     *
     * <p><b>Note:</b> {@code returnType} must not be a generic {@link javax.lang.model.type.TypeMirror}.
     * Converting a {@link javax.lang.model.type.TypeMirror} to a {@link javax.lang.model.element.TypeElement} will
     * lose implementation-specific type information. For example, generic class parameters (like {@code K}, {@code V})
     * will be used instead type variables, even when converted back to {@link javax.lang.model.type.TypeMirror}.</p>
     *
     * @param returnType the type whose generic parameters are to be mapped.
     * @return a {@link java.util.Map} mapping each generic {@link javax.lang.model.type.TypeMirror} to its corresponding
     *         concrete {@link javax.lang.model.type.TypeMirror}. Returns an empty map if {@code returnType}
     *         is not a {@link javax.lang.model.type.DeclaredType}.
     */
    public static Map<TypeMirror, TypeMirror> makeParamTypeMap(TypeMirror returnType) {
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
}
