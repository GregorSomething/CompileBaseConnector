package me.gregorsomething.database.processor.types;

import com.squareup.javapoet.CodeBlock;
import lombok.RequiredArgsConstructor;
import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.processor.RepositoryProcessor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

@RequiredArgsConstructor
public class TypeMapperCodeGenerator {
    private final RepositoryProcessor processor;
    private final TypeMapperResolver typeMapperResolver;

    public CodeBlock forResultSet(ExecutableElement element, Query query) {
        // TODO pure resultset return (ResultSet)
        return null;
    }

    public CodeBlock forOptional(ExecutableElement element, Query query, TypeMirror optionalType) {
        // TODO Optional (Optional<Long>, Optional<String>)
        return null;
    }

    public CodeBlock forList(ExecutableElement element, Query query, TypeMirror listElementType) {
        // TODO list (List<Long>, List<String>)
        return null;
    }

    public CodeBlock forSimpleType(ExecutableElement element, Query query) {
        // TODO Simple Types or extention types (Long, String...) AND Primitive types like int, long
        return null;
    }

    private String databaseQueryArgsFor(ExecutableElement element) {
        StringBuilder args = new StringBuilder();
        for (VariableElement parameter : element.getParameters()) {
            args.append(", ").append(parameter.getSimpleName().toString());
        }
        return args.toString();
    }
}
