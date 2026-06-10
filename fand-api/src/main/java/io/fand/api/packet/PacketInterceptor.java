package io.fand.api.packet;

/**
 * Callback invoked for every matching packet before it is handled or sent.
 *
 * <p><strong>Thread semantics:</strong> interceptors run on whichever thread
 * produced the packet. Serverbound packets are intercepted on the network or
 * packet-processing thread; clientbound packets are intercepted on the thread
 * that called {@code send} (often the server thread, but also network or
 * worker threads). Implementations must therefore be thread-safe, must not
 * block, and must not touch world state, entities, or inventories directly.
 * Use {@code Scheduler.runMain} to hand work to the server thread instead.
 */
@FunctionalInterface
public interface PacketInterceptor<T extends PacketView> {

    void intercept(PacketController<T> packet);
}
