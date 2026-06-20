package io.fand.api.packet;

import io.fand.api.entity.Player;
import io.fand.api.player.PlayerProfile;
import java.net.SocketAddress;
import java.util.Optional;

/**
 * Runtime connection context for a packet callback.
 */
public interface PacketContext {

    PacketProtocol protocol();

    PacketDirection direction();

    Optional<? extends Player> player();

    /**
     * Connection identity when known. In PLAY this mirrors {@link #player()};
     * during LOGIN it may be present before a Player object exists.
     */
    default Optional<PlayerProfile> profile() {
        return player().map(Player::profile);
    }

    Optional<SocketAddress> remoteAddress();
}
