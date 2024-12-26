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
            return CodeBlock.builder().build(); // TODO primitive (int, long)
        }
        if (this.processor.isOfType(type, ResultSet.class)) {
            return CodeBlock.builder().build(); // TODO pure resultset return (ResultSet)
        }
        if (this.processor.isBaseTypeOf(type, Optional.class)) {
            return CodeBlock.builder().build(); // TODO Optional (Optional<Long>, Optional<String>)
        }
        if (this.processor.isBaseTypeOf(type, List.class)) {
            return CodeBlock.builder().build(); // TODO list (List<Long>, List<String>)
        }
        return CodeBlock.builder().build(); // TODO Simple Types or extention types (Long, String...)
    }
}
