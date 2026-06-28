package io.fand.api.packet;

import io.fand.api.block.BlockType;
import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Per-viewer client illusions backed by clientbound packets.
 */
public interface ViewerIllusionService {

    boolean sendPacket(Player viewer, PacketView packet);

    boolean fakeBlock(Player viewer, Location location, BlockType type);

    boolean fakeEntity(Player viewer, PacketView spawnPacket);

    boolean removeFakeEntity(Player viewer, int entityId);

    default boolean hideEntity(Player viewer, Entity entity) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(entity, "entity");
        viewer.hideEntity(entity);
        return true;
    }

    default boolean showEntity(Player viewer, Entity entity) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(entity, "entity");
        viewer.showEntity(entity);
        return true;
    }

    static ViewerIllusionService unsupported() {
        return new ViewerIllusionService() {
            @Override
            public boolean sendPacket(Player viewer, PacketView packet) {
                throw new UnsupportedOperationException("Viewer illusions are not supported");
            }

            @Override
            public boolean fakeBlock(Player viewer, Location location, BlockType type) {
                throw new UnsupportedOperationException("Viewer illusions are not supported");
            }

            @Override
            public boolean fakeEntity(Player viewer, PacketView spawnPacket) {
                throw new UnsupportedOperationException("Viewer illusions are not supported");
            }

            @Override
            public boolean removeFakeEntity(Player viewer, int entityId) {
                throw new UnsupportedOperationException("Viewer illusions are not supported");
            }
        };
    }
}
