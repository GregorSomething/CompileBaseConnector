package me.gregorsomething.database.processor.types;

import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.processor.ProcessingValidationException;
import me.gregorsomething.database.processor.RepositoryProcessor;
import me.gregorsomething.database.processor.helpers.Pair;
import me.gregorsomething.database.processor.paramater.ParameterProcessor;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ComplexTypeResolver {

    private final RepositoryProcessor processor;
    private final ComplexTypeMapperGenerator generator;
    private final TypeMapperResolver typeMapperResolver;

    public ComplexTypeResolver(RepositoryProcessor processor, TypeMapperResolver typeMapperResolver) {
        this.processor = processor;
        this.typeMapperResolver = typeMapperResolver;
        this.generator = new ComplexTypeMapperGenerator(processor, typeMapperResolver);
    }

    /**
     * Resolves mapper for complex types
     * @param element method to implement
     * @param query sqlQuery to analyze
     * @return method name that accepts result set
     */
    public String tryResolveType(ExecutableElement element, Query query, TypeMirror returnType) {
        String methodName = this.getMethodNameFor(element, 0);
        this.queryToMapper(returnType, query.value(), element, methodName);
        return methodName;
    }

    private String getMethodNameFor(ExecutableElement element, int extra) {
        String methodName = element.getSimpleName().toString() + "Mapper" + (extra == 0 ? "" : extra);
        if (this.typeMapperResolver.getCurrentRepoBuilder().methodSpecs
                .stream().anyMatch(m -> m.name.equalsIgnoreCase(methodName)))
            return this.getMethodNameFor(element, extra + 1);
        else
            return methodName;
    }

    private void queryToMapper(TypeMirror type, String query, ExecutableElement element, String methodName) {
        String pureQuery = ParameterProcessor.removePlaceholders(query);
        List<Pair<Integer, String>> selectedItems = this.getSelectedItemNames(pureQuery, element);
        Element returnType = this.processor.getTypeUtils().asElement(type);
        if (returnType instanceof TypeElement typeElement) {
            if (this.findSuitableStaticMethodFromClass(typeElement, selectedItems, methodName))
                return; // Successfully found and generated method to mapped, can return method name
            if (this.findSuitableConstructorFromClass(typeElement, selectedItems, methodName))
                return; // Successfully found and generated method to mapped, can return method name
            throw new ProcessingValidationException("Auto type mapper failed to find suitable instantiation method", element);
        }
        throw new ProcessingValidationException("Auto type mapper does not recognise type kind (not TypeElement): "
                + returnType.getKind().toString(), element);
    }

    private boolean findSuitableConstructorFromClass(TypeElement returnType, List<Pair<Integer, String>> selectedItems, String methodName) {
        Set<String> itemNames = selectedItems.stream().map(Pair::right).map(String::toLowerCase).collect(Collectors.toSet());
        Optional<ExecutableElement> constructor = this.hasTypeMatchingConstructor(returnType, itemNames);
        if (constructor.isEmpty())
            return false;
        this.generator.generateMapperMethodForTypeWithConstructor(returnType, selectedItems, methodName, constructor.get());
        return true;
    }

    private boolean findSuitableStaticMethodFromClass(TypeElement returnType, List<Pair<Integer, String>> selectedItems, String methodName) {
        Set<String> itemNames = selectedItems.stream().map(Pair::right).map(String::toLowerCase).collect(Collectors.toSet());
        Optional<ExecutableElement> method = this.hasTypeMatchingStaticMethod(returnType, itemNames);
        if (method.isEmpty())
            return false;
        this.generator.generateMapperMethodForTypeWithStaticMethod(returnType, selectedItems, methodName, method.get());
        return true;
    }

    private List<Pair<Integer, String>> getSelectedItemNames(String query, ExecutableElement element) {
        try {
            Statement parsed = CCJSqlParserUtil.parse(query);
            if (parsed instanceof PlainSelect select) {
                List<Pair<Integer, String>> result = new ArrayList<>();
                int index = 1;
                for (SelectItem<?> selectItem : select.getSelectItems()) {
                    result.add(Pair.of(index++, this.selectItemToString(selectItem, element)));
                }
                return result;
            } else {
                throw new ProcessingValidationException("Only plain selects are allowed", element);
            }
        } catch (JSQLParserException e) {
            throw new ProcessingValidationException("Query is not valid!", element);
        }
    }

    private String selectItemToString(SelectItem<?> item, ExecutableElement element) {
        if (item.getExpression() instanceof AllColumns) {
            throw new ProcessingValidationException("Complex query does not support wildcard", element);
        } else if (item.getAliasName() != null) {
            return item.getAliasName();
        } else if (item.getExpression() instanceof Column column) {
            return column.getColumnName();
        } else {
            throw new ProcessingValidationException("Use 'as' or select column directly", element);
        }
    }

    private Optional<ExecutableElement> hasTypeMatchingConstructor(TypeElement type, Set<String> itemNames) {
        List<ExecutableElement> constructors = ElementFilter.constructorsIn(type.getEnclosedElements());
        return constructors.stream()
                .filter(c -> c.getModifiers().contains(Modifier.PUBLIC))
                .filter(c -> {
                    Set<String> constructorArgs = c.getParameters().stream()
                            .map(p -> p.getSimpleName().toString().toLowerCase())
                            .collect(Collectors.toSet());
                    return constructorArgs.size() == itemNames.size() && constructorArgs.containsAll(itemNames);
                }).findAny();
    }

    private Optional<ExecutableElement> hasTypeMatchingStaticMethod(TypeElement returnType, Set<String> itemNames) {
        return ElementFilter.methodsIn(returnType.getEnclosedElements()).stream()
                .filter(m -> m.getModifiers().containsAll(List.of(Modifier.PUBLIC, Modifier.STATIC)))
                .filter(m -> this.processor.getTypeUtils().isAssignable(m.getReturnType(), returnType.asType()))
                .filter(m -> {
                    Set<String> paramArgs = m.getParameters().stream()
                            .map(p -> p.getSimpleName().toString().toLowerCase())
                            .collect(Collectors.toSet());
                    return paramArgs.size() == itemNames.size() && paramArgs.containsAll(itemNames);
                }).findAny();

    }
}
