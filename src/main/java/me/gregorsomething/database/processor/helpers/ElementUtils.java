package me.gregorsomething.database.processor.helpers;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
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
}
