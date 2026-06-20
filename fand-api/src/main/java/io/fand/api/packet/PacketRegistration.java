package io.fand.api.packet;

/**
 * Handle for a packet listener, interceptor, or custom channel registration.
 *
 * <p>{@link #unregister()} removes only the resource this handle installed.
 * If a newer registration for the same channel/packet type was installed
 * afterwards, closing this handle must not remove it; closing this handle on
 * an already-closed registration is a no-op.
 */
public interface PacketRegistration extends AutoCloseable {

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
