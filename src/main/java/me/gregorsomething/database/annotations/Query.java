package me.gregorsomething.database.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface Query {
    /**
     * Sql statement that is used for query
     * @return query sql statement
     */
    String value();

    /**
     * Used as literal in return statement like 'return ENTERED_VALUE'
     * @return return default value
     */
    String defaultValue() default "";

    /**
     * On no rows returned should it throw exception
     * @return if true, error is thrown, else default value gets returned
     */
    boolean onNoResultThrow() default true;
}
