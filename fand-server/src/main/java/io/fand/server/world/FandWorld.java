package io.fand.server.world;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.api.world.particle.ParticleEffect;
import io.fand.api.world.particle.ParticleEmission;
import io.fand.api.world.sound.SoundEffect;
import io.fand.server.block.FandBlock;
import io.fand.server.entity.PlayerRegistry;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
        return players.snapshot(handle);
    }

    @Override
    public Iterable<? extends Audience> audiences() {
        return players();
    }

    @Override
    public void playSound(Location location, SoundEffect sound) {
        Objects.requireNonNull(sound, "sound");
        var checkedLocation = requireThisWorld(location);
        runOnServerThread(() -> SoundEffects.play(handle, checkedLocation, sound));
    }

    @Override
    public void spawnParticle(Location location, ParticleEffect effect, ParticleEmission emission) {
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(emission, "emission");
        var checkedLocation = requireThisWorld(location);
        runOnServerThread(() -> ParticleEffects.spawn(handle, checkedLocation, effect, emission));
    }

    @Override
    public Block blockAt(int x, int y, int z) {
        return new FandBlock(this, x, y, z);
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

    private Location requireThisWorld(Location location) {
        Objects.requireNonNull(location, "location");
        if (!location.world().key().equals(key)) {
            throw new IllegalArgumentException("Location world " + location.world().key().asString()
                    + " does not match " + key.asString());
        }
        return location;
    }

    private void runOnServerThread(Runnable task) {
        var server = handle.getServer();
        if (server == null || server.isSameThread()) {
            task.run();
        } else {
            server.executeIfPossible(task);
        }
    }
}
