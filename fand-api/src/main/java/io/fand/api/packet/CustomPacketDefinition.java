package io.fand.api.packet;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Declares a custom payload channel for play or configuration traffic.
 */
public record CustomPacketDefinition(PacketProtocol protocol, PacketDirection direction, Key channel) {

    public CustomPacketDefinition {
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(channel, "channel");
        if (protocol != PacketProtocol.PLAY && protocol != PacketProtocol.CONFIGURATION) {
            throw new IllegalArgumentException("Custom payload channels are only supported in play and configuration protocols");
        }
    }

    public static CustomPacketDefinition play(PacketDirection direction, Key channel) {
        return new CustomPacketDefinition(PacketProtocol.PLAY, direction, channel);
    }

    public static CustomPacketDefinition configuration(PacketDirection direction, Key channel) {
        return new CustomPacketDefinition(PacketProtocol.CONFIGURATION, direction, channel);
    }
}
