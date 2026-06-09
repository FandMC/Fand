package io.fand.api.packet;

import java.util.Arrays;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Raw custom payload packet.
 */
public record CustomPacket(Key channel, byte[] payload) {

    public CustomPacket {
        Objects.requireNonNull(channel, "channel");
        payload = Objects.requireNonNull(payload, "payload").clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CustomPacket packet
                && channel.equals(packet.channel)
                && Arrays.equals(payload, packet.payload);
    }

    @Override
    public int hashCode() {
        return 31 * channel.hashCode() + Arrays.hashCode(payload);
    }
}
