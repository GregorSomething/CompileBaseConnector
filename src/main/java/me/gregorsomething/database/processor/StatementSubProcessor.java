package me.gregorsomething.database.processor;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import lombok.RequiredArgsConstructor;
import me.gregorsomething.database.annotations.Statement;
import me.gregorsomething.database.processor.helpers.Pair;
import me.gregorsomething.database.processor.paramater.ParameterProcessor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import java.sql.SQLException;

@RequiredArgsConstructor
public class StatementSubProcessor {
    private final RepositoryProcessor processor;
    private final ParameterProcessor parameterProcessor;

    public MethodSpec createStatementMethod(ExecutableElement element) {
        Statement statement = this.validateStatementAnnotationOn(element);
        return this.processor.validateAndOverrideMethod(element)
                .addCode(this.generateCodeFor(element, statement))
                .build();
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
        Pair<String, String> queryParams = this.parameterProcessor.queryParametersFor(element, statement.value());
        if (!element.getThrownTypes().isEmpty())
            return CodeBlock.builder()
                    .addStatement("this.database.execute($S$L)", queryParams.left(), queryParams.right())
                    .build();
        return CodeBlock.builder()
                .beginControlFlow("try")
                .addStatement("this.database.execute($S$L)", queryParams.left(), queryParams.right())
                .nextControlFlow("catch ($T e)", SQLException.class)
                .addStatement("throw new $T(e)", RuntimeException.class)
                .endControlFlow()
                .build();
    }
}
