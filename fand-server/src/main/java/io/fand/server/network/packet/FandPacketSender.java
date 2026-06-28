package io.fand.server.network.packet;

import io.fand.api.entity.Player;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketSender;
import io.fand.api.packet.PacketView;
import io.fand.server.entity.FandPlayer;
import io.fand.server.util.ServerThreading;
import java.util.Objects;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

final class FandPacketSender implements PacketSender {

    private final VanillaPacketBridge bridge;

    FandPacketSender(VanillaPacketBridge bridge) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    @Override
    public boolean send(Player viewer, PacketView packet) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(packet, "packet");
        if (packet.packetType().direction() != PacketDirection.CLIENTBOUND) {
            throw new IllegalArgumentException("Only clientbound packets can be sent to players");
        }
        var handle = handle(viewer);
        if (handle.hasDisconnected() || handle.connection == null) {
            return false;
        }
        var vanilla = bridge.toVanilla(packet);
        if (vanilla == null) {
            return false;
        }
        return send(handle, vanilla);
    }

    static ServerPlayer handle(Player viewer) {
        if (viewer instanceof FandPlayer fandPlayer) {
            return fandPlayer.handle();
        }
        throw new IllegalArgumentException("Player must be a server-backed FandPlayer");
    }

    static boolean send(ServerPlayer player, Packet<?> packet) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packet, "packet");
        if (player.hasDisconnected() || player.connection == null) {
            return false;
        }
        return ServerThreading.run(player.level().getServer(), () -> {
            if (!player.hasDisconnected() && player.connection != null) {
                player.connection.send(packet);
            }
        });
    }
}
