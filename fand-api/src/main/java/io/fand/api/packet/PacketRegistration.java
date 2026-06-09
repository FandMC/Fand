package io.fand.api.packet;

/**
 * Handle for a packet listener, interceptor, or custom channel registration.
 */
public interface PacketRegistration extends AutoCloseable {

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
