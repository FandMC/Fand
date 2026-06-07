package io.fand.server.world;

import io.fand.api.block.Block;
import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.world.ParticlePlayback;
import io.fand.api.world.SoundPlayback;
import io.fand.api.world.World;
import io.fand.server.block.FandBlock;
import io.fand.server.entity.PlayerRegistry;
import io.fand.server.hooks.FandHooks;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class FandWorld implements World {

    private static final int DEFAULT_WEATHER_TICKS = 6000;

    private final ServerLevel handle;
    private final Key key;
    private final @Nullable PlayerRegistry players;

    public FandWorld(ServerLevel handle) {
        this(handle, null);
    }

    public FandWorld(ServerLevel handle, @Nullable PlayerRegistry players) {
        this.handle = handle;
        this.players = players;
        var identifier = handle.dimension().identifier();
        this.key = Key.key(identifier.getNamespace(), identifier.getPath());
    }

    public ServerLevel handle() {
        return handle;
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public long seed() {
        return handle.getSeed();
    }

    @Override
    public Collection<? extends Player> players() {
        if (players == null) {
            return List.of();
        }
        var snapshot = new ArrayList<Player>();
        for (var candidate : players.snapshot()) {
            if (candidate.handle().level() == handle) {
                snapshot.add(candidate);
            }
        }
        return List.copyOf(snapshot);
    }

    @Override
    public Collection<? extends Entity> entities() {
        var registry = FandHooks.entities();
        if (registry.isEmpty()) {
            return List.of();
        }
        var snapshot = new ArrayList<Entity>();
        for (var entity : handle.getAllEntities()) {
            snapshot.add(registry.get().wrap(entity));
        }
        return List.copyOf(snapshot);
    }

    @Override
    public Iterable<? extends Audience> audiences() {
        return players();
    }

    @Override
    public Block blockAt(int x, int y, int z) {
        return new FandBlock(this, x, y, z);
    }

    @Override
    public void spawnParticle(ParticlePlayback playback) {
        ParticleEffects.spawnParticle(handle, playback);
    }

    @Override
    public void playSound(SoundPlayback playback) {
        SoundEffects.playSound(handle, playback);
    }

    @Override
    public long time() {
        return handle.dimensionType()
                .defaultClock()
                .map(clock -> handle.clockManager().getTotalTicks(clock))
                .orElse(0L);
    }

    @Override
    public void setTime(long time) {
        if (time < 0L) {
            throw new IllegalArgumentException("time must be >= 0, got " + time);
        }
        var server = handle.getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> handle.dimensionType()
                .defaultClock()
                .ifPresent(clock -> server.clockManager().setTotalTicks(clock, time));
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
    }

    @Override
    public boolean dayNightCycleEnabled() {
        return handle.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME);
    }

    @Override
    public void setDayNightCycleEnabled(boolean enabled) {
        var server = handle.getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> handle.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, enabled, server);
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
    }

    @Override
    public boolean storming() {
        return handle.isRaining();
    }

    @Override
    public void setStorming(boolean storming) {
        setStorming(storming, DEFAULT_WEATHER_TICKS);
    }

    @Override
    public void setStorming(boolean storming, Duration duration) {
        setStorming(storming, durationToTicks(duration));
    }

    @Override
    public void setStorming(boolean storming, int ticks) {
        requireNonNegativeTicks(ticks, "ticks");
        var server = handle.getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> {
            var weatherData = handle.getWeatherData();
            weatherData.setClearWeatherTime(storming ? 0 : ticks);
            weatherData.setRainTime(ticks);
            weatherData.setRaining(storming);
        };
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
    }

    @Override
    public boolean thundering() {
        return handle.isThundering();
    }

    @Override
    public void setThundering(boolean thundering) {
        setThundering(thundering, DEFAULT_WEATHER_TICKS);
    }

    @Override
    public void setThundering(boolean thundering, Duration duration) {
        setThundering(thundering, durationToTicks(duration));
    }

    @Override
    public void setThundering(boolean thundering, int ticks) {
        requireNonNegativeTicks(ticks, "ticks");
        var server = handle.getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> {
            var weatherData = handle.getWeatherData();
            weatherData.setThunderTime(ticks);
            weatherData.setThundering(thundering);
            if (thundering && !handle.isRaining()) {
                weatherData.setClearWeatherTime(0);
                weatherData.setRainTime(ticks);
                weatherData.setRaining(true);
            }
        };
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
    }

    private static int durationToTicks(Duration duration) {
        java.util.Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must be non-negative, got " + duration);
        }
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, duration.toMillis() / 50L));
    }

    private static void requireNonNegativeTicks(int ticks, String name) {
        if (ticks < 0) {
            throw new IllegalArgumentException(name + " must be >= 0, got " + ticks);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandWorld that && this.key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "FandWorld(" + key.asString() + ")";
    }
}
