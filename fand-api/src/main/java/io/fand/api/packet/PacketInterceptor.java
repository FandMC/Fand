package io.fand.api.packet;

/**
 * Inspects, cancels, or replaces a single vanilla packet.
 *
 * <p>Interceptors run on the Netty I/O thread of the owning connection, never
 * on the main server thread. Implementations must not block and must not touch
 * world or entity state directly; marshal such work to the server thread via
 * the scheduler.
 *
 * @param <V> the view type for the {@link PacketType} this interceptor is
 *            registered against — either a typed sub-interface of
 *            {@link PacketView} or {@code PacketView} itself
 */
@FunctionalInterface
public interface PacketInterceptor<V extends PacketView> {

    /**
     * Handles one packet. Read fields from {@code view} (typed accessors or the
     * dynamic {@link PacketView#get}); call {@link PacketController#cancel} to
     * drop it, or {@link PacketController#replace} with {@code view.with(...)}
     * to substitute modified field values.
     *
     * @param view       an immutable, NMS-free view of the packet's fields
     * @param controller controls the packet's fate; valid only for this call
     */
    void intercept(V view, PacketController controller);
}
