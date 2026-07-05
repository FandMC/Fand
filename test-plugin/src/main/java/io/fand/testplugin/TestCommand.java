package io.fand.testplugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface TestCommand {
    String label();

    String[] arguments() default {};

    String[] aliases() default {};

    String permission() default "";
}
