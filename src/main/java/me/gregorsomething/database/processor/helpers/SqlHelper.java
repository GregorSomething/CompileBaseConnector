package me.gregorsomething.database.processor.helpers;

import lombok.RequiredArgsConstructor;
import me.gregorsomething.database.processor.ProcessingValidationException;
import me.gregorsomething.database.processor.RepositoryProcessor;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.time.*;

@RequiredArgsConstructor
public class SqlHelper {
    private final RepositoryProcessor processor;

    public String simpleMapperFor(TypeMirror type, int index) {
        if (type.getKind().isPrimitive())
            return this.getPrimitiveMapperFor(type.getKind(), index);
        return simpleObjectMapperFor(type, index);
    }

    private String getPrimitiveMapperFor(TypeKind type, int index) {
        return switch (type) {
            case BOOLEAN -> "rs.getBoolean(%d)".formatted(index);
            case BYTE -> "rs.getByte(%d)".formatted(index);
            case SHORT -> "rs.getShort(%d)".formatted(index);
            case INT -> "rs.getInt(%d)".formatted(index);
            case LONG -> "rs.getLong(%d)".formatted(index);
            case CHAR -> "rs.getChar(%d)".formatted(index);
            case FLOAT -> "rs.getFloat(%d)".formatted(index);
            case DOUBLE -> "rs.getDouble(%d)".formatted(index);
            default -> throw new ProcessingValidationException("Expected primitive type but found none", null);
        };
    }

    private String simpleObjectMapperFor(TypeMirror type, int index) {
        if (this.processor.isOfType(type, String.class)) {
            return "rs.getString(%d)".formatted(index);
        }
        if (this.processor.isOfType(type, LocalDateTime.class)) {
            return "rs.getTimestamp(%d).toLocalDateTime()".formatted(index);
        }
        if (this.processor.isOfType(type, Instant.class)) {
            return "rs.getTimestamp(%d).toInstant()".formatted(index);
        }
        if (this.processor.isOfType(type, LocalDate.class)) {
            return "rs.getDate(%d).toLocalDate()".formatted(index);
        }
        if (this.processor.isOfType(type, LocalTime.class)) {
            return "rs.getTime(%d).toLocalTime()".formatted(index);
        }
        try {
            PrimitiveType primitiveType = this.processor.getTypeUtils().unboxedType(type);
            return getPrimitiveMapperFor(primitiveType.getKind(), index);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
