package me.gregorsomething.database.processor;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import lombok.RequiredArgsConstructor;
import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.processor.helpers.ElementUtils;
import me.gregorsomething.database.processor.types.TypeMapperCodeGenerator;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class QuerySubProcessor {
    private final RepositoryProcessor processor;
    private final TypeMapperCodeGenerator typeMapperCodeGenerator;

    public MethodSpec createQueryMethod(ExecutableElement element) {
        Query query = this.validateQueryAnnotationOn(element);
        return this.processor.validateAndOverrideMethod(element)
                .addCode(this.generateCodeFor(element, query))
                .build();
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
        if (type.getKind().isPrimitive()) {
            // Must be here, because primitives cause unexpected issues in other comparisons
            return this.typeMapperCodeGenerator.forType(element, query);
        }
        if (this.processor.isOfType(type, ResultSet.class)) {
            return this.typeMapperCodeGenerator.forResultSet(element, query);
        }
        if (this.processor.isBaseTypeOf(type, Optional.class)) {
            return this.typeMapperCodeGenerator.forOptional(element, query,
                    ElementUtils.getTypeParameterOf(element.getReturnType(), element));
        }
        if (this.processor.isBaseTypeOf(type, List.class)) {
            return this.typeMapperCodeGenerator.forList(element, query,
                    ElementUtils.getTypeParameterOf(element.getReturnType(), element));
        }
        return this.typeMapperCodeGenerator.forType(element, query);
    }
}
