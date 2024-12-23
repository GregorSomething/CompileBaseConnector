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
}
