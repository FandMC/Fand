package io.fand.api.resourcepack;

/**
 * Lifecycle handle for a managed resource pack.
 */
public interface ResourcePackRegistration extends AutoCloseable {

    String id();

    boolean active();

    void delete();

    @Override
    default void close() {
        delete();
    }
}
