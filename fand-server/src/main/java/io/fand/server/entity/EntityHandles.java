package io.fand.server.entity;

final class EntityHandles {

    private EntityHandles() {
    }

    static net.minecraft.world.entity.Entity unwrap(io.fand.api.entity.Entity entity) {
        if (entity instanceof FandEntity fandEntity) {
            return fandEntity.handle();
        }
        if (entity instanceof FandPlayer player) {
            return player.handle();
        }
        throw new IllegalArgumentException("Entity is not owned by this server: " + entity.uniqueId());
    }
}
