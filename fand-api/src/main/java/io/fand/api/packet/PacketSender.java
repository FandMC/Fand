package io.fand.api.packet;

import io.fand.api.entity.Player;
import java.util.Collection;
import java.util.Objects;

/**
 * Sends already-built clientbound packet views to online players.
 */
public interface PacketSender {

    boolean send(Player viewer, PacketView packet);

    default int send(Collection<? extends Player> viewers, PacketView packet) {
        Objects.requireNonNull(viewers, "viewers");
        Objects.requireNonNull(packet, "packet");
        int sent = 0;
        for (var viewer : viewers) {
            if (send(viewer, packet)) {
                sent++;
            }
        }
        return sent;
    }

    static PacketSender unsupported() {
        return (viewer, packet) -> {
            throw new UnsupportedOperationException("Direct packet sends are not supported");
        };
    }
}
