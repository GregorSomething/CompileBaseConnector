package me.gregorsomething.database.processor.types;

import lombok.RequiredArgsConstructor;
import me.gregorsomething.database.annotations.Repository;
import me.gregorsomething.database.processor.ProcessingValidationException;
import me.gregorsomething.database.processor.RepositoryProcessor;
import me.gregorsomething.database.processor.helpers.Pair;

import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class TypeMapperResolver {

    private final RepositoryProcessor processor;
    private final Map<TypeMirror, Pair<TypeMirror, String>> typesMapped = new HashMap<>();

    /**
     * Finds repository typedef mappings based on its annotation
     * @param repo annotation for what repo to find mappings
     */
    public void setup(Repository repo) {
        for (TypeMirror type : this.readClasses(repo)) {
            this.searchFromExtension(type);
        }
    }

    /**
     * Checks if this has found typedef with return value of key
     * @param type type for what to look mapper
     * @return true if found else false
     */
    public boolean hasTypeDefFromResultSet(TypeMirror type) {
        return this.typesMapped.containsKey(type);
    }

    /**
     * Returns mapped type class type and method name, signature is name(ResultSet, int), return type is key
     * @param type return type for mapper
     * @return mapper class and method name if present
     */
    public Pair<TypeMirror, String> getTypeMapperFromResultSet(TypeMirror type) {
        return this.typesMapped.get(type);
    }

    /**
     * Checks if type need null check with rs.wasNull() (types like Long are casted to long at some point,
     * removing null, and long is not nullable, so to return def value if it was null in database,
     * we have to check if it was null before)
     * @param type type to check for null check need
     * @return true if type needs null check
     */
    public boolean needsNullCheck(TypeMirror type) {
        if (type.getKind().isPrimitive())
            return true; // is int, long, byte
        try {
            // Only care about exception, if thrown it was not primitive value boxed
            this.processor.getTypeUtils().unboxedType(type);
            return true; // is Long, Integer, Byte
        } catch (IllegalArgumentException ignored) {
            return false; // is anything else
        }
    }

    /**
     * Returns type mapping for simple type like Long, String, LocalDateTime, long, int
     * NOTE! Null checks and stuff is not included, this gives info only about result set to type
     * @param type type to what look mapping
     * @return pair of methods, first is for ResultSet, and second, optional, for result of ResultSet mapping
     */
    public Pair<String, String> getBuiltinMapperForType(TypeMirror type) {
        if (type.getKind().isPrimitive())
            return new Pair<>(this.getResultSetMethodForPrimitive(type.getKind()), null);
        return this.getResultSetMethodForSimpleType(type);
    }

    /**
     * Returns ResultSet method name to read primitive type
     * @param type primitive type TypeKind
     * @return method name like getInt
     */
    private String getResultSetMethodForPrimitive(TypeKind type) {
        return switch (type) {
            case BOOLEAN -> "getBoolean";
            case BYTE -> "getByte";
            case SHORT -> "getShort";
            case INT -> "getInt";
            case LONG -> "getLong";
            case CHAR -> "getChar";
            case FLOAT -> "getFloat";
            case DOUBLE -> "getDouble";
            default -> throw new ProcessingValidationException("Expected primitive type but found none", null);
        };
    }

    /**
     * Returns type mapping for simple type like Long, String, LocalDateTime
     * @param type type to what look mapping
     * @return pair of methods, first is for ResultSet, and second, optional, for result of ResultSet mapping
     */
    private Pair<String, String> getResultSetMethodForSimpleType(TypeMirror type) {
        if (this.processor.isOfType(type, String.class)) {
            return new Pair<>("getString", null);
        }
        if (this.processor.isOfType(type, LocalDateTime.class)) {
            return new Pair<>("getTimestamp", "toLocalDateTime()");
        }
        if (this.processor.isOfType(type, Instant.class)) {
            return new Pair<>("getTimestamp", "toInstant()");
        }
        if (this.processor.isOfType(type, LocalDate.class)) {
            return new Pair<>("getDate", "toLocalDate()");
        }
        if (this.processor.isOfType(type, LocalTime.class)) {
            return new Pair<>("getTime", "toLocalTime()");
        }
        try {
            // Long -> long unboxing
            PrimitiveType primitiveType = this.processor.getTypeUtils().unboxedType(type);
            return new Pair<>(this.getResultSetMethodForPrimitive(primitiveType.getKind()), null);
        } catch (IllegalArgumentException ignored) {
            // Ignored
        }
        throw new ProcessingValidationException("Unknown type: " + type.toString() + " in some repository.", null);
    }

    private void searchFromExtension(TypeMirror classType) {
        Element element = this.processor.getTypeUtils().asElement(classType);
        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            this.processor.error("Type extension for repository must be public!", element);
        }
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.METHOD)) {
                // Last check checks that this is method.
                this.processMethod((ExecutableElement) enclosedElement, element);
            }
        }
    }

    private void processMethod(ExecutableElement element, Element parent) {
        if (!this.isCorrectSignatureForTypeDef(element, parent))
            return;
        this.typesMapped.put(element.getReturnType(), new Pair<>(parent.asType(), element.getSimpleName().toString()));
    }

    private boolean isCorrectSignatureForTypeDef(ExecutableElement element, Element parent) {
        // Check that element is public static
        if (parent.getKind().equals(ElementKind.INTERFACE)) {
            if (!element.getModifiers().contains(Modifier.DEFAULT)) {
                return false;
            }
        } else if (!element.getModifiers().contains(Modifier.PUBLIC)
                || !element.getModifiers().contains(Modifier.STATIC)
                || element.getModifiers().contains(Modifier.ABSTRACT)) {
            return false;
        }
        // Check parameter signature
        List<? extends VariableElement> params = element.getParameters();
        if (params.size() == 2
                && this.processor.isOfType(params.get(0).asType(), ResultSet.class)
                && params.get(1).asType().getKind().equals(TypeKind.INT)) {
            // Returns true if no exception or if only one and it is SQLException
            return element.getThrownTypes().isEmpty() ||
                    element.getThrownTypes().size() == 1
                            && this.processor.isBaseTypeOf(element.getThrownTypes().getFirst(), SQLException.class);
        }
        this.processor.warning("Invalid signature for type def to a repository", element);
        return false;
    }

    private List<? extends TypeMirror> readClasses(Repository repo) {
        try {
            // https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation
            // This should almost always cause exception,
            // as this might not be compiled,
            // but just in case this is handed here as well
            return Arrays.stream(repo.additionalTypes())
                    .map(c -> this.processor.getElementUtils()
                            .getTypeElement(c.getTypeName())
                            .asType())
                    .toList();
        } catch (MirroredTypesException ex) {
            return ex.getTypeMirrors();
        }
    }
}
