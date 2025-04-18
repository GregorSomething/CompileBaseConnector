package me.gregorsomething.database.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Query {
    /**
     * Sql statement that is used for query
     * @return query sql statement
     */
    String value();

    /**
     * Used as literal in return statement like 'return ENTERED_VALUE'
     * When return type is list or optional this is not directly returns,
     * as {@code List.of()} or {@code Optional.empty()} will be returned.
     * This is ignored when return type is Optional or when type does
     * not require null check after reading from ResultSet (primitives and boxed-primitives)
     * @return return default value
     */
    String defaultValue() default "null";

    /**
     * On no rows returned should it throw exception
     * @return if true, error is thrown, else default value gets returned
     */
    boolean onNoResultThrow() default false;
}
