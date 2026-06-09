package io.fand.api.packet;

import io.fand.api.entity.Player;
import java.net.SocketAddress;
import java.util.Optional;

/**
 * Runtime connection context for a packet callback.
 */
public interface PacketContext {

    PacketProtocol protocol();

    PacketDirection direction();

    Optional<? extends Player> player();

    Optional<SocketAddress> remoteAddress();
}
