package me.gregorsomething.database.processor.helpers;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import me.gregorsomething.database.processor.ProcessingValidationException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;

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
}
