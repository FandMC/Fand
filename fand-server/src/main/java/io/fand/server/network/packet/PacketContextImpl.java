package io.fand.server.network.packet;

import io.fand.api.entity.Player;
import io.fand.api.packet.PacketContext;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketProtocol;
import io.fand.api.player.PlayerProfile;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

final class PacketContextImpl implements PacketContext {

    private final PacketProtocol protocol;
    private final PacketDirection direction;
    private final Optional<? extends Player> player;
    private final Optional<PlayerProfile> profile;
    private final Optional<SocketAddress> remoteAddress;

    PacketContextImpl(
            PacketProtocol protocol,
            PacketDirection direction,
            Optional<? extends Player> player,
            Optional<PlayerProfile> profile,
            @Nullable SocketAddress remoteAddress
    ) {
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.player = Objects.requireNonNull(player, "player");
        this.profile = Objects.requireNonNull(profile, "profile")
                .or(() -> player.map(Player::profile));
        this.remoteAddress = Optional.ofNullable(remoteAddress);
    }

    @Override
    public PacketProtocol protocol() {
        return protocol;
    }

    @Override
    public PacketDirection direction() {
        return direction;
    }

    @Override
    public Optional<? extends Player> player() {
        return player;
    }

    @Override
    public Optional<PlayerProfile> profile() {
        return profile;
    }

    @Override
    public Optional<SocketAddress> remoteAddress() {
        return remoteAddress;
    }
}
