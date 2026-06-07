package io.fand.api.packet;

import io.fand.api.entity.Player;

/**
 * Handles a custom packet arriving from a player (client to server).
 *
 * <p>Handlers run on the Netty I/O thread of the sending player's connection,
 * not the main server thread. Marshal any world or entity interaction to the
 * server thread.
 *
 * @param <P> the payload record type
 */
@FunctionalInterface
public interface CustomPacketHandler<P extends Record> {

    /**
     * Handles a decoded payload received from {@code sender}.
     *
     * @param sender  the player whose connection delivered the packet
     * @param payload the decoded payload
     */
    void handle(Player sender, P payload);
}
