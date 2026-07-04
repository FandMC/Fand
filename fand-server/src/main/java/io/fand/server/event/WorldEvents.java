package io.fand.server.event;

import io.fand.api.event.world.ChunkLoadEvent;
import io.fand.api.event.world.ChunkUnloadEvent;
import io.fand.api.event.world.SpawnChangeEvent;
import io.fand.api.event.world.ThunderChangeEvent;
import io.fand.api.event.world.TimeSkipEvent;
import io.fand.api.event.world.WeatherChangeEvent;
import io.fand.api.event.world.WorldSaveEvent;
import io.fand.api.world.Location;
import io.fand.server.hooks.FandHooks;
import io.fand.server.world.FandWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorldEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldEvents.class);

    private WorldEvents() {
    }

    public static void fireSave(ServerLevel level) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(WorldSaveEvent.class)) {
            return;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return;
        }
        try {
            bus.fire(new WorldSaveEvent(world));
        } catch (RuntimeException failure) {
            LOGGER.warn("WorldSaveEvent listener failed", failure);
        }
    }

    public static boolean fireWeatherChanges(MinecraftServer server, boolean raining, boolean thundering) {
        var bus = FandHooks.events();
        boolean weatherListeners = bus.hasListeners(WeatherChangeEvent.class);
        boolean thunderListeners = bus.hasListeners(ThunderChangeEvent.class);
        if (!weatherListeners && !thunderListeners) {
            return true;
        }

        var weatherData = server.getWeatherData();
        boolean wasRaining = weatherData.isRaining();
        boolean wasThundering = weatherData.isThundering();
        if (wasRaining == raining && wasThundering == thundering) {
            return true;
        }

        for (ServerLevel level : server.getAllLevels()) {
            var world = FandHooks.wrapWorld(level);
            if (world == null) {
                continue;
            }
            try {
                if (weatherListeners && wasRaining != raining) {
                    var event = new WeatherChangeEvent(world, wasRaining, raining);
                    bus.fire(event);
                    if (event.cancelled()) {
                        return false;
                    }
                }
                if (thunderListeners && wasThundering != thundering) {
                    var event = new ThunderChangeEvent(world, wasThundering, thundering);
                    bus.fire(event);
                    if (event.cancelled()) {
                        return false;
                    }
                }
            } catch (RuntimeException failure) {
                LOGGER.warn("Weather listener failed for {}", world.key().asString(), failure);
            }
        }
        return true;
    }

    public static void fireChunkStatus(ServerLevel level, ChunkPos pos, FullChunkStatus status) {
        if (status == FullChunkStatus.FULL) {
            fireChunkLoad(level, pos);
        } else if (status == FullChunkStatus.INACCESSIBLE) {
            fireChunkUnload(level, pos);
        }
    }

    public static Long fireTimeSkip(ServerLevel level, TimeSkipEvent.Cause cause, long fromTime, long toTime) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(TimeSkipEvent.class)) {
            return toTime;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return toTime;
        }
        var event = new TimeSkipEvent(world, cause, fromTime, toTime);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("TimeSkipEvent listener failed for {}", world.key().asString(), failure);
            return toTime;
        }
        return event.cancelled() ? null : event.toTime();
    }

    public static LevelData.@Nullable RespawnData fireSpawnChange(
            MinecraftServer server,
            LevelData.RespawnData previous,
            LevelData.RespawnData next
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(SpawnChangeEvent.class)) {
            return next;
        }
        var previousLevel = server.getLevel(previous.dimension());
        var nextLevel = server.getLevel(next.dimension());
        if (previousLevel == null || nextLevel == null) {
            return next;
        }
        var previousWorld = FandHooks.wrapWorld(previousLevel);
        var nextWorld = FandHooks.wrapWorld(nextLevel);
        if (previousWorld == null || nextWorld == null) {
            return next;
        }
        var event = new SpawnChangeEvent(location(previousWorld, previous), location(nextWorld, next));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("SpawnChangeEvent listener failed", failure);
            return next;
        }
        if (event.cancelled()) {
            return null;
        }
        var retargeted = event.newSpawn();
        ServerLevel targetLevel = resolveLevel(server, retargeted.world());
        if (targetLevel == null) {
            LOGGER.warn("SpawnChangeEvent targeted an unloaded world: {}", retargeted.world().key().asString());
            return next;
        }
        return LevelData.RespawnData.of(
                targetLevel.dimension(),
                BlockPos.containing(retargeted.x(), retargeted.y(), retargeted.z()),
                retargeted.yaw(),
                retargeted.pitch());
    }

    private static void fireChunkLoad(ServerLevel level, ChunkPos pos) {
        var bus = FandHooks.events();
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return;
        }
        FandHooks.customBlocks().handleChunkLoaded(world, pos.x(), pos.z());
        if (!bus.hasListeners(ChunkLoadEvent.class)) {
            return;
        }
        try {
            bus.fire(new ChunkLoadEvent(world.chunkAt(pos.x(), pos.z())));
        } catch (RuntimeException failure) {
            LOGGER.warn("ChunkLoadEvent listener failed", failure);
        }
    }

    private static void fireChunkUnload(ServerLevel level, ChunkPos pos) {
        var bus = FandHooks.events();
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return;
        }
        FandHooks.markRedstoneChunkDirty(level, pos.x(), pos.z(), "chunk-unload");
        FandHooks.customBlocks().handleChunkUnloaded(world, pos.x(), pos.z());
        if (!bus.hasListeners(ChunkUnloadEvent.class)) {
            return;
        }
        try {
            bus.fire(new ChunkUnloadEvent(world.chunkAt(pos.x(), pos.z())));
        } catch (RuntimeException failure) {
            LOGGER.warn("ChunkUnloadEvent listener failed", failure);
        }
    }

    private static Location location(FandWorld world, LevelData.RespawnData data) {
        var pos = data.pos();
        return new Location(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, data.yaw(), data.pitch());
    }

    private static ServerLevel resolveLevel(MinecraftServer server, io.fand.api.world.World world) {
        if (world instanceof FandWorld fandWorld) {
            return fandWorld.handle();
        }
        var key = world.key();
        for (var level : server.getAllLevels()) {
            var identifier = level.dimension().identifier();
            if (identifier.getNamespace().equals(key.namespace()) && identifier.getPath().equals(key.value())) {
                return level;
            }
        }
        return null;
    }
}
