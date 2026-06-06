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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class FandWorld implements World {

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
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                snapshot.add(registry.get().wrap(living));
            }
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
        return handle.getLevelData().getGameTime();
    }

    @Override
    public void setTime(long time) {
        var server = handle.getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> {
            if (handle.getLevelData() instanceof net.minecraft.world.level.storage.ServerLevelData serverData) {
                serverData.setGameTime(time);
            }
        };
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
        var server = handle.getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> {
            var weatherData = handle.getWeatherData();
            weatherData.setRainTime(storming ? 6000 : 0);
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
        var server = handle.getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> {
            var weatherData = handle.getWeatherData();
            weatherData.setThunderTime(thundering ? 6000 : 0);
            weatherData.setThundering(thundering);
            if (thundering && !handle.isRaining()) {
                weatherData.setRainTime(6000);
                weatherData.setRaining(true);
            }
        };
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
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
