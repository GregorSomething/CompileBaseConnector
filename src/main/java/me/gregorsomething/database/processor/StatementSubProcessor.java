package me.gregorsomething.database.processor;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.gregorsomething.database.annotations.Statement;
import me.gregorsomething.database.processor.helpers.ElementUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.sql.SQLException;

@RequiredArgsConstructor
public class StatementSubProcessor {
    private final RepositoryProcessor processor;

    public MethodSpec createStatementMethod(ExecutableElement element) {
        Statement statement = this.validateStatementAnnotationOn(element);
        MethodSpec.Builder builder = ElementUtils.overrideMethod(element, Modifier.PUBLIC);

        if (element.getThrownTypes().size() > 1)
            throw new ProcessingValidationException("Only one exception can be thrown", element);
        if (element.getThrownTypes().size() == 1 && !this.processor.isOfType(element.getThrownTypes().getFirst(), SQLException.class))
            throw new ProcessingValidationException("Only SQLException can be thrown", element);
        if (element.getThrownTypes().size() == 1)
            builder.addException(SQLException.class);
        else
            builder.addAnnotation(SneakyThrows.class);

        builder.addCode(this.generateCodeFor(element, statement));

        return builder.build();
    }

    private Statement validateStatementAnnotationOn(ExecutableElement element) {
        if (!element.getReturnType().getKind().equals(TypeKind.VOID))
            throw new ProcessingValidationException("Statement cant have return type!", element);
        Statement[] annotations = element.getAnnotationsByType(Statement.class);
        if (annotations.length != 1)
            throw new ProcessingValidationException("Statement annotation can be use once on an element!", element);

        return annotations[0];
    }

    private CodeBlock generateCodeFor(ExecutableElement element, Statement statement) {
        CodeBlock.Builder builder = CodeBlock.builder();
        StringBuilder args = new StringBuilder();
        for (VariableElement parameter : element.getParameters()) {
            args.append(", ").append(parameter.getSimpleName().toString());
        }
        builder.addStatement("this.database.execute($S$L)", statement.value(), args.toString());
        return builder.build();
    }
}
