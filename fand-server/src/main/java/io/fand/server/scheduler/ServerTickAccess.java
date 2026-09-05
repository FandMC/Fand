package io.fand.server.scheduler;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import io.fand.api.entity.Entity;
import io.fand.api.world.Location;
import io.fand.server.entity.FandEntity;
import io.fand.server.entity.FandPlayer;
import io.fand.server.world.FandWorld;

/** Validates targets in the current single-owner server execution model. */
final class ServerTickAccess {

    private final Supplier<@Nullable MinecraftServer> server;

    ServerTickAccess(Supplier<@Nullable MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    void global() {
        currentServer();
    }

    void at(Location location) {
        var current = currentServer();
        if (!(location.world() instanceof FandWorld world)) {
            throw new IllegalArgumentException("World is not owned by this server");
        }
        requireLoaded(current, world.handle());
    }

    void forEntity(Entity entity) {
        var current = currentServer();
        var handle = switch (entity) {
            case FandPlayer player -> player.handle();
            case FandEntity other -> other.handle();
            default -> throw new IllegalArgumentException("Entity is not owned by this server");
        };
        if (!(handle.level() instanceof ServerLevel level)) {
            throw new IllegalStateException("Scheduled entity is not in a loaded server world");
        }
        requireLoaded(current, level);
        if (handle.isRemoved() || level.getEntity(handle.getUUID()) != handle) {
            throw new IllegalStateException("Scheduled entity is no longer active");
        }
    }

    private MinecraftServer currentServer() {
        var current = server.get();
        if (current == null) {
            throw new IllegalStateException("Minecraft server is not attached");
        }
        if (!current.isSameThread()) {
            throw new IllegalStateException("Owned tasks must execute on the server tick thread");
        }
        return current;
    }

    private static void requireLoaded(MinecraftServer server, ServerLevel level) {
        if (level.getServer() != server || server.getLevel(level.dimension()) != level) {
            throw new IllegalStateException("Scheduled world is no longer loaded");
        }
    }
}
