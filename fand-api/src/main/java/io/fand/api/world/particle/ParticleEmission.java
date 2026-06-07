package io.fand.api.world.particle;

/** Packet-level particle emission parameters shared by all particle effects. */
public record ParticleEmission(
        int count,
        double offsetX,
        double offsetY,
        double offsetZ,
        double speed,
        boolean overrideLimiter,
        boolean alwaysShow) {

    public static final ParticleEmission SINGLE = new ParticleEmission(1, 0.0, 0.0, 0.0, 0.0, false, false);

    public ParticleEmission {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
        requireFinite(offsetX, "offsetX");
        requireFinite(offsetY, "offsetY");
        requireFinite(offsetZ, "offsetZ");
        requireFinite(speed, "speed");
    }

    public static ParticleEmission count(int count) {
        return new ParticleEmission(count, 0.0, 0.0, 0.0, 0.0, false, false);
    }

    public ParticleEmission withCount(int count) {
        return new ParticleEmission(count, offsetX, offsetY, offsetZ, speed, overrideLimiter, alwaysShow);
    }

    public ParticleEmission withOffset(double offsetX, double offsetY, double offsetZ) {
        return new ParticleEmission(count, offsetX, offsetY, offsetZ, speed, overrideLimiter, alwaysShow);
    }

    public ParticleEmission withSpeed(double speed) {
        return new ParticleEmission(count, offsetX, offsetY, offsetZ, speed, overrideLimiter, alwaysShow);
    }

    public ParticleEmission withOverrideLimiter(boolean overrideLimiter) {
        return new ParticleEmission(count, offsetX, offsetY, offsetZ, speed, overrideLimiter, alwaysShow);
    }

    public ParticleEmission withAlwaysShow(boolean alwaysShow) {
        return new ParticleEmission(count, offsetX, offsetY, offsetZ, speed, overrideLimiter, alwaysShow);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
