package me.gregorsomething.database.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Repository {
    /**
     * Database create statements, that can be run many times
     * @return create statements
     */
    String[] value() default {};

    /**
     * Classes from witch to look for additional type defs. These methods must be static and have "signature" like
     * <code>public static T from(ResultSet rs, int pos) throws SQLException</code>
     * They also might contain hole row mapping like
     * <i>public static T from(ResultSet rs, int startingPos) throws SQLException</i>
     * @return classes from what processor searches types.
     */
    Class<?>[] additionalTypes() default {};
}
