package io.fand.api.world;

import io.fand.api.entity.Player;
import io.fand.api.world.particle.ParticleEffect;
import io.fand.api.world.particle.ParticleEmission;
import io.fand.api.world.sound.SoundEffect;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;

/**
 * A loaded world (dimension) on the server. Identified by a {@link Key} matching
 * the underlying Minecraft dimension key (e.g. {@code minecraft:overworld}).
 *
 * <p>World handles are stable for as long as the dimension stays loaded; equality
 * is by {@link #key()}. Methods that touch world state must be called on the server
 * thread unless explicitly documented as thread-safe.
 *
 * <p>{@code World} is an Adventure {@link ForwardingAudience} that forwards to
 * the players currently in this world.
 */
public interface World extends ForwardingAudience {

    /** Dimension key, e.g. {@code minecraft:overworld}. */
    Key key();

    /** Convenience for {@code key().asString()}. */
    default String name() {
        return key().asString();
    }

    /** World seed. */
    long seed();

    /** Total game ticks elapsed for this world. */
    long gameTime();

    /** Sets total game ticks for this world. Marshals to the server thread. */
    CompletableFuture<Void> setGameTime(long ticks);

    /** Current default clock ticks for this world. */
    long time();

    /** Sets default clock ticks for this world. Marshals to the server thread. */
    CompletableFuture<Void> setTime(long ticks);

    /** Current server difficulty. */
    Difficulty difficulty();

    /** Sets server difficulty. Marshals to the server thread. */
    CompletableFuture<Void> setDifficulty(Difficulty difficulty);

    /** Whether rain is active. */
    boolean storm();

    /** Sets rain state. Marshals to the server thread. */
    CompletableFuture<Void> setStorm(boolean storm);

    /** Whether thunder is active. */
    boolean thundering();

    /** Sets thunder state. Marshals to the server thread. */
    CompletableFuture<Void> setThundering(boolean thundering);

    /** Live world border controls. */
    WorldBorder worldBorder();

    /** Saves this world. Marshals to the server thread. */
    CompletableFuture<Boolean> save();

    /** Snapshot of all players currently in this world. */
    Collection<? extends Player> players();

    /** Plays a sound at {@code location} for players in this world. Marshals to the server thread. */
    void playSound(Location location, SoundEffect sound);

    /** Spawns a single particle at {@code location} for players in this world. Marshals to the server thread. */
    default void spawnParticle(Location location, ParticleEffect effect) {
        spawnParticle(location, effect, ParticleEmission.SINGLE);
    }

    /** Spawns particles at {@code location} for players in this world. Marshals to the server thread. */
    void spawnParticle(Location location, ParticleEffect effect, ParticleEmission emission);

    /** Builds a {@link Location} in this world. */
    default Location at(double x, double y, double z) {
        return new Location(this, x, y, z, 0.0F, 0.0F);
    }

    /** Builds a {@link Location} in this world with rotation. */
    default Location at(double x, double y, double z, float yaw, float pitch) {
        return new Location(this, x, y, z, yaw, pitch);
    }

    /**
     * Returns a positional block handle. The handle is lazy — it does not read
     * the world until {@link io.fand.api.block.Block#type()} or
     * {@link io.fand.api.block.Block#setType} is invoked.
     */
    io.fand.api.block.Block blockAt(int x, int y, int z);
}
