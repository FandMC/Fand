package io.fand.api.placeholder;

public interface PlaceholderRegistration extends AutoCloseable {

    String namespace();

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
