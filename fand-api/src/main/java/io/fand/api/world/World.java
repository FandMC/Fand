package io.fand.api.world;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import java.time.Duration;
import java.util.Collection;
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
public interface World extends ForwardingAudience, ParticleEmitter {

    /** Dimension key, e.g. {@code minecraft:overworld}. */
    Key key();

    /** Convenience for {@code key().asString()}. */
    default String name() {
        return key().asString();
    }

    /** World seed. */
    long seed();

    /** Snapshot of all players currently in this world. */
    Collection<? extends Player> players();

    /** Snapshot of all entities currently in this world. */
    Collection<? extends Entity> entities();

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

    /**
     * Spawns particles using the full playback parameter set. All players within
     * range see the effect. Marshals to the main thread when called off-thread.
     *
     * @param playback particle type/data, position, count, spread, speed, and force flag
     */
    @Override
    void spawnParticle(ParticlePlayback playback);

    /**
     * Plays a sound using the full playback parameter set. All players within
     * range hear it, or all players in this world when {@link SoundPlayback#minVolume()}
     * is positive. Marshals to the main thread when called off-thread.
     *
     * @param playback sound, position, category, volume, pitch, min volume, and seed
     */
    void playSound(SoundPlayback playback);

    /**
     * Current default clock time in ticks. For vanilla overworld-like dimensions
     * this is the day/night clock used by {@code /time set}, where 0-23999 is a
     * full cycle; the raw value may exceed 23999 after multiple days. Dimensions
     * without a default clock return {@code 0}.
     */
    long time();

    /**
     * Sets the default clock time. Marshals to the main thread when called
     * off-thread. No-ops for dimensions without a default clock. Negative times
     * are rejected.
     */
    void setTime(long time);

    /**
     * Whether the server-wide day/night cycle gamerule is enabled. When
     * {@code false}, world clocks do not advance naturally. Minecraft 26.1.2
     * stores this as a global gamerule even though it is exposed here for
     * convenience from a world handle.
     */
    boolean dayNightCycleEnabled();

    /**
     * Sets the server-wide day/night cycle gamerule. Marshals to the main thread.
     */
    void setDayNightCycleEnabled(boolean enabled);

    /**
     * Whether weather is currently active (rain/snow). Thunder is separate.
     */
    boolean storming();

    /**
     * Sets whether it is raining or snowing using the server default weather
     * duration. Marshals to the main thread.
     */
    void setStorming(boolean storming);

    /**
     * Sets whether it is raining or snowing for {@code duration}. Marshals to
     * the main thread. Negative durations are rejected.
     */
    void setStorming(boolean storming, Duration duration);

    /**
     * Sets whether it is raining or snowing for {@code ticks}. Marshals to the
     * main thread. Negative durations are rejected.
     */
    void setStorming(boolean storming, int ticks);

    /**
     * Whether thunder is currently active. Only meaningful when
     * {@link #storming()} is {@code true}.
     */
    boolean thundering();

    /**
     * Sets whether thunder is active. Marshals to the main thread. Has no
     * visible effect unless {@link #storming()} is also {@code true}. Uses the
     * server default weather duration.
     */
    void setThundering(boolean thundering);

    /**
     * Sets whether thunder is active for {@code duration}. Marshals to the main
     * thread. Negative durations are rejected.
     */
    void setThundering(boolean thundering, Duration duration);

    /**
     * Sets whether thunder is active for {@code ticks}. Marshals to the main
     * thread. Negative durations are rejected.
     */
    void setThundering(boolean thundering, int ticks);
}
