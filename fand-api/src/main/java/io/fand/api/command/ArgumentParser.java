package io.fand.api.command;

/**
 * Converts a raw command token into an API value.
 *
 * @param <T> parsed value type
 */
@FunctionalInterface
public interface ArgumentParser<T> {

    T parse(String input) throws Exception;
}
