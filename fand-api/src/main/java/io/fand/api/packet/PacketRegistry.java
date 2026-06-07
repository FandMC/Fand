package io.fand.api.packet;

import io.fand.api.Fand;
import io.fand.api.entity.Player;

/**
 * Entry point for vanilla packet interception and custom packet channels.
 *
 * <p>Obtain the instance via {@link #get()}.
 */
public interface PacketRegistry {

    /** Returns the running server's packet registry. */
    static PacketRegistry get() {
        return Fand.server().packets();
    }

    /**
     * Registers {@code interceptor} for every packet of {@code type}. The
     * interceptor's view parameter must match {@link PacketType#viewType()}.
     *
     * <p>The interceptor runs on the connection's Netty I/O thread.
     *
     * @param <V> the view type, which must be assignable from
     *            {@link PacketType#viewType()} for {@code type}; use
     *            {@link PacketView} for packets without a dedicated typed view
     * @return a handle that removes the interceptor when closed
     */
    <V extends PacketView> PacketRegistration intercept(PacketType type, PacketInterceptor<V> interceptor);

    /**
     * Registers a custom packet definition together with a handler for inbound
     * payloads. For an {@link PacketDirection#OUTBOUND} definition, pass
     * {@code null} for {@code handler}.
     *
     * <p><strong>Reserved namespaces.</strong> The {@code bungeecord} and
     * {@code velocity} namespaces carry proxy plugin messaging that Fand must
     * never intercept, alter, or claim; routing those through a user handler
     * would silently break proxy forwarding. Registration whose definition uses
     * either namespace is therefore rejected.
     *
     * @return a handle that removes the definition and handler when closed
     * @throws IllegalArgumentException if the definition's namespace is
     *         {@code bungeecord} or {@code velocity}, or if {@code handler} is
     *         {@code null} for an inbound definition
     */
    <P extends Record> PacketRegistration register(
            CustomPacketDefinition<P> definition,
            CustomPacketHandler<P> handler);

    /**
     * Sends a custom packet to {@code player}. The payload's type must match a
     * registered {@link PacketDirection#OUTBOUND} (or bidirectional)
     * definition.
     *
     * <p>May be called from any thread; the send is marshalled onto the
     * player's connection event loop.
     *
     * @throws IllegalArgumentException if no outbound definition is registered
     *         for {@code payload}'s type
     */
    <P extends Record> void send(Player player, P payload);
}
