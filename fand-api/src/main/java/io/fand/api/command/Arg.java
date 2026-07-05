package io.fand.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Arg {
    String value();

    CommandArgumentType type() default CommandArgumentType.WORD;

    boolean optional() default false;

    boolean optionalSender() default false;

    String defaultValue() default "";

    int defaultInt() default 0;

    long defaultLong() default 0L;

    double defaultDouble() default 0.0D;

    boolean defaultBoolean() default false;

    int min() default Integer.MIN_VALUE;

    int max() default Integer.MAX_VALUE;

    String[] suggestions() default {};
}
