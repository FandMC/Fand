package io.fand.server.entity;

import io.fand.server.util.ReflectionFields;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;

final class EntityVisibility {

    private static final Field ENTITY_MAP = ReflectionFields.field(ChunkMap.class, "entityMap");
    private static final Class<?> TRACKED_ENTITY_CLASS = trackedEntityClass();
    private static final Field SEEN_BY = ReflectionFields.field(TRACKED_ENTITY_CLASS, "seenBy");
    private static final Method REMOVE_PLAYER = ReflectionFields.method(
            TRACKED_ENTITY_CLASS, "removePlayer", ServerPlayer.class);
    private static final Method UPDATE_PLAYER = ReflectionFields.method(
            TRACKED_ENTITY_CLASS, "updatePlayer", ServerPlayer.class);

    private EntityVisibility() {
    }

    static void hide(ServerPlayer viewer, net.minecraft.world.entity.Entity entity) {
        var tracked = trackedEntity(entity);
        if (tracked != null) {
            ReflectionFields.invoke(REMOVE_PLAYER, tracked, viewer);
        }
    }

    static void show(ServerPlayer viewer, net.minecraft.world.entity.Entity entity) {
        var tracked = trackedEntity(entity);
        if (tracked != null) {
            ReflectionFields.invoke(UPDATE_PLAYER, tracked, viewer);
        }
    }

    static boolean trackedBy(ServerPlayer viewer, net.minecraft.world.entity.Entity entity) {
        var tracked = trackedEntity(entity);
        if (tracked == null) {
            return false;
        }
        return ((Set<?>) ReflectionFields.value(SEEN_BY, tracked)).contains(viewer.connection);
    }

    private static Object trackedEntity(net.minecraft.world.entity.Entity entity) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        var entityMap = (Int2ObjectMap<Object>) ReflectionFields.value(ENTITY_MAP, level.getChunkSource().chunkMap);
        return entityMap.get(entity.getId());
    }

    private static Class<?> trackedEntityClass() {
        for (var nested : ChunkMap.class.getDeclaredClasses()) {
            if (nested.getSimpleName().equals("TrackedEntity")) {
                return nested;
            }
        }
        throw new IllegalStateException("Missing ChunkMap.TrackedEntity");
    }
}
