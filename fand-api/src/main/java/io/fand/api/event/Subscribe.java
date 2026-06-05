package io.fand.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method on a {@link Listener} as an event handler. The method must
 * return {@code void} and take exactly one parameter whose type extends
 * {@link Event}; the parameter type is the event being subscribed to.
 *
 * <p>Subscribed methods may have any visibility; non-public methods are made
 * accessible during registration.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {

    EventPriority priority() default EventPriority.NORMAL;
}
