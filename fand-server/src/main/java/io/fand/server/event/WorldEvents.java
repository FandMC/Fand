package io.fand.server.event;

import io.fand.api.event.world.ChunkLoadEvent;
import io.fand.api.event.world.ChunkUnloadEvent;
import io.fand.api.event.world.ThunderChangeEvent;
import io.fand.api.event.world.WeatherChangeEvent;
import io.fand.api.event.world.WorldSaveEvent;
import io.fand.server.hooks.FandHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
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

    private static void fireChunkLoad(ServerLevel level, ChunkPos pos) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(ChunkLoadEvent.class)) {
            return;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return;
        }
        try {
            bus.fire(new ChunkLoadEvent(world, pos.x(), pos.z()));
        } catch (RuntimeException failure) {
            LOGGER.warn("ChunkLoadEvent listener failed", failure);
        }
    }

    private static void fireChunkUnload(ServerLevel level, ChunkPos pos) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(ChunkUnloadEvent.class)) {
            return;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return;
        }
        try {
            bus.fire(new ChunkUnloadEvent(world, pos.x(), pos.z()));
        } catch (RuntimeException failure) {
            LOGGER.warn("ChunkUnloadEvent listener failed", failure);
        }
    }
}
