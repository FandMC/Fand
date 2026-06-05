package io.fand.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.Nullable;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandSpec {
    String label();

    String namespace() default "";

    String[] subcommands() default {};

    String[] aliases() default {};

    @Nullable String permission() default "";
}
