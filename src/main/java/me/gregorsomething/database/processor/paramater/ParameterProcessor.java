package me.gregorsomething.database.processor.paramater;

import lombok.RequiredArgsConstructor;
import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.processor.RepositoryProcessor;
import me.gregorsomething.database.processor.helpers.Pair;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ParameterProcessor {

    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("\\[\\([^\\[]+\\)]");

    private final RepositoryProcessor processor;

    /**
     * Replaces placeholders in SQL query with '?'
     * @param sqlQuery query where to remove placeholders
     * @return query without placeholders
     */
    public static String removePlaceholders(String sqlQuery) {
        return PLACEHOLDER_REGEX.matcher(sqlQuery).replaceAll("?");
    }

    /**
     * Processes sql query
     * @param element element for what query is about
     * @param query sql qquery string
     * @return SQL query string after processing and parameters seperated by coma, including first
     */
    public Pair<String, String> queryParametersFor(ExecutableElement element, Query query) {
        String sqlQuery = query.value();
        List<String> replaceable = this.findReplaceable(sqlQuery);

        if (replaceable.isEmpty()) {
            return this.queryParametersDirectlyFromElement(element, query);
        }
        if (sqlQuery.contains("?")) {
            this.processor.error("If query has [(...)] parameter it must not contain ? parameters", element);
        }
        return this.queryParametersFromElementProcessing(element, query, replaceable);
    }

    /**
     * Processes [(...)] to usable formats
     */
    private Pair<String, String> queryParametersFromElementProcessing(ExecutableElement element, Query query, List<String> toProcess) {
        String newQuery = PLACEHOLDER_REGEX.matcher(query.value()).replaceAll("?");

        if (element.getParameters().isEmpty()) {
            this.processor.error("If query has [(...)] parameter, method must have parameters", element);
            throw new UnsupportedOperationException("Processing terminated, no parameters");
        }

        Map<String, List<Pair<String, String>>> foundElements = new HashMap<>();
        for (VariableElement parameter : element.getParameters()) {
            foundElements.put(parameter.getSimpleName().toString(), this.getPublicVariables(parameter));
        }
        List<String> mappingsForProcessed = new ArrayList<>();
        for (String process : toProcess) {
            mappingsForProcessed.add(this.findAndResolveMappingFor(foundElements, process));
        }

        return Pair.of(newQuery, mappingsForProcessed.stream().map(s -> ", " + s).collect(Collectors.joining()));
    }

    private String findAndResolveMappingFor(Map<String, List<Pair<String, String>>> foundElements, String process) {
        if (!process.contains("."))
            return process;
        String[] split = process.split("\\.");
        // If not expected value return
        if (split.length != 2) {
            return process;
        }

        if (!foundElements.containsKey(split[0]))
            return process;
        Optional<Pair<String, String>> pair = foundElements.get(split[0]).stream()
                .filter(p -> p.left().equals(split[1]))
                .findFirst();
        return pair.map(p -> split[0] + "." + p.right()).orElse(process);
    }

    private List<Pair<String, String>> getPublicVariables(VariableElement parameter) {
        Element element = this.processor.getTypeUtils().asElement(parameter.asType());
        List<Pair<String, String>> res = new ArrayList<>();
        if (element.getKind().isClass() || element.getKind().isInterface()) {
            List<ExecutableElement> methods = ElementFilter.methodsIn(element.getEnclosedElements());
            List<String> list = methods.stream()
                    .filter(m ->
                            !m.getModifiers().contains(Modifier.PRIVATE)
                                    && !m.getModifiers().contains(Modifier.PROTECTED)
                                    && !m.getModifiers().contains(Modifier.STATIC))
                    .filter(m -> !m.getReturnType().getKind().equals(TypeKind.VOID))
                    .filter(m -> m.getParameters().isEmpty())
                    .map(m -> m.getSimpleName().toString())
                    .toList();
            list.forEach(name -> {
                final String methodName = name + "()";
                res.add(Pair.of(name, methodName));
                res.add(Pair.of(name + "()", methodName));
                String nameWithoutGet = name.replaceFirst("get", "");
                res.add(Pair.of(nameWithoutGet, methodName));
                res.add(Pair.of(nameWithoutGet.toLowerCase(Locale.ROOT), methodName));
            });
        }
        if (element.getKind().isClass()) {
            List<VariableElement> elements = ElementFilter.fieldsIn(element.getEnclosedElements());
            List<String> list = elements.stream()
                    .filter(m ->
                            !m.getModifiers().contains(Modifier.PRIVATE)
                                    && !m.getModifiers().contains(Modifier.PROTECTED)
                                    && !m.getModifiers().contains(Modifier.STATIC))
                    .map(m -> m.getSimpleName().toString())
                    .toList();
            list.forEach(name -> res.add(Pair.of(name, name)));
        }
        return res;
    }

    private Pair<String, String> queryParametersDirectlyFromElement(ExecutableElement element, Query query) {
        StringBuilder args = new StringBuilder();
        for (VariableElement parameter : element.getParameters()) {
            args.append(", ").append(parameter.getSimpleName().toString());
        }
        return Pair.of(query.value(), args.toString());
    }

    private List<String> findReplaceable(String query) {
        Matcher matcher = PLACEHOLDER_REGEX.matcher(query);

        List<String> res = new ArrayList<>();
        while (matcher.find()) {
            res.add(matcher.group());
        }

        return res.stream()
                .map(s -> s.substring(2, s.length() - 2).strip())
                .toList();
    }
}
