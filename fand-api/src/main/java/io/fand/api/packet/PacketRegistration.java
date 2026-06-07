package io.fand.api.packet;

/**
 * Handle to a single packet registration. Closing it removes the interceptor,
 * handler, or definition it represents.
 *
 * <p>Idempotent: closing more than once has no further effect.
 */
public interface PacketRegistration extends AutoCloseable {

    @Override
    void close();
}
