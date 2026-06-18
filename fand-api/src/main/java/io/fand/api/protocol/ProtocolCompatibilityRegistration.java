package io.fand.api.protocol;

public interface ProtocolCompatibilityRegistration extends AutoCloseable {

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
