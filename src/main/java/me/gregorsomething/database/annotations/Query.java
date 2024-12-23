package me.gregorsomething.database.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface Query {
    String value();
    String mapping() default "";
    String defaultValue() default "";
}
