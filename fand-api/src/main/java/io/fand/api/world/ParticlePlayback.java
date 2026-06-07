package io.fand.api.world;

import java.util.Objects;

/**
 * Complete parameters for spawning a particle burst.
 *
 * <p>{@code count} controls how many particles are emitted. Offsets describe
 * the random spread around the source position, and {@code speed} is passed to
 * vanilla as the particle's speed/extra value. {@code force} bypasses the
 * client's distance limiter for long-range or important visual effects.
 */
public record ParticlePlayback(
        Particle particle,
        double x,
        double y,
        double z,
        int count,
        double offsetX,
        double offsetY,
        double offsetZ,
        double speed,
        boolean force
) {

    public ParticlePlayback {
        Objects.requireNonNull(particle, "particle");
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(offsetX, "offsetX");
        requireFinite(offsetY, "offsetY");
        requireFinite(offsetZ, "offsetZ");
        requireFinite(speed, "speed");
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0, got " + count);
        }
    }

    public static ParticlePlayback of(Particle particle, Location location) {
        Objects.requireNonNull(location, "location");
        return of(particle, location.x(), location.y(), location.z());
    }

    public static ParticlePlayback of(Particle particle, double x, double y, double z) {
        return new ParticlePlayback(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0, false);
    }

    public ParticlePlayback count(int count) {
        return new ParticlePlayback(particle, x, y, z, count, offsetX, offsetY, offsetZ, speed, force);
    }

    public ParticlePlayback offset(double offsetX, double offsetY, double offsetZ) {
        return new ParticlePlayback(particle, x, y, z, count, offsetX, offsetY, offsetZ, speed, force);
    }

    public ParticlePlayback speed(double speed) {
        return new ParticlePlayback(particle, x, y, z, count, offsetX, offsetY, offsetZ, speed, force);
    }

    public ParticlePlayback force(boolean force) {
        return new ParticlePlayback(particle, x, y, z, count, offsetX, offsetY, offsetZ, speed, force);
    }

    public ParticlePlayback move(double dx, double dy, double dz) {
        return new ParticlePlayback(particle, x + dx, y + dy, z + dz, count, offsetX, offsetY, offsetZ, speed, force);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite, got " + value);
        }
    }
}
