package io.fand.api.datapack;

/**
 * Lifecycle handle for a managed data pack.
 */
public interface DataPackRegistration extends AutoCloseable {

    String id();

    boolean active();

    void enable();

    void disable();

    void delete();

    @Override
    default void close() {
        delete();
    }
}
