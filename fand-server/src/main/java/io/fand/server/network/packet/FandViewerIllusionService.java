package io.fand.server.network.packet;

import io.fand.api.block.BlockType;
import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketView;
import io.fand.api.packet.ViewerIllusionService;
import io.fand.api.world.Location;
import io.fand.server.block.FandBlockType;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;

final class FandViewerIllusionService implements ViewerIllusionService {

    private final FandPacketSender sender;

    FandViewerIllusionService(FandPacketSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    @Override
    public boolean sendPacket(Player viewer, PacketView packet) {
        return sender.send(viewer, packet);
    }

    @Override
    public boolean fakeBlock(Player viewer, Location location, BlockType type) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(type, "type");
        var handle = FandPacketSender.handle(viewer);
        if (handle.hasDisconnected() || handle.connection == null || !sameWorld(handle, location)) {
            return false;
        }
        var pos = new BlockPos(location.blockX(), location.blockY(), location.blockZ());
        var state = FandBlockType.unwrap(type).defaultBlockState();
        return FandPacketSender.send(handle, new ClientboundBlockUpdatePacket(pos, state));
    }

    @Override
    public boolean fakeEntity(Player viewer, PacketView spawnPacket) {
        Objects.requireNonNull(spawnPacket, "spawnPacket");
        if (spawnPacket.packetType().direction() != PacketDirection.CLIENTBOUND) {
            throw new IllegalArgumentException("Only clientbound packets can be sent as fake entity packets");
        }
        return sender.send(viewer, spawnPacket);
    }

    @Override
    public boolean removeFakeEntity(Player viewer, int entityId) {
        Objects.requireNonNull(viewer, "viewer");
        var handle = FandPacketSender.handle(viewer);
        if (handle.hasDisconnected() || handle.connection == null) {
            return false;
        }
        return FandPacketSender.send(handle, new ClientboundRemoveEntitiesPacket(entityId));
    }

    @Override
    public boolean hideEntity(Player viewer, Entity entity) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(entity, "entity");
        viewer.hideEntity(entity);
        return true;
    }

    @Override
    public boolean showEntity(Player viewer, Entity entity) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(entity, "entity");
        viewer.showEntity(entity);
        return true;
    }

    private static boolean sameWorld(net.minecraft.server.level.ServerPlayer player, Location location) {
        var key = location.world().key();
        var dimension = player.level().dimension().identifier();
        return dimension.getNamespace().equals(key.namespace()) && dimension.getPath().equals(key.value());
    }
}
