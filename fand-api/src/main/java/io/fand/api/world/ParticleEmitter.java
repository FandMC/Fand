package io.fand.api.world;

/** Target that can receive fully described particle playbacks. */
@FunctionalInterface
public interface ParticleEmitter {

    /** Spawns one particle playback on this target. */
    void spawnParticle(ParticlePlayback playback);

    /**
     * Samples a mathematical function from {@code from} to {@code to} and
     * spawns one particle at each sampled point relative to {@code origin}.
     */
    default void plotParticle(Particle particle, Location origin, ParticleFunction function,
                              double from, double to, int samples) {
        if (samples <= 0) {
            throw new IllegalArgumentException("samples must be > 0, got " + samples);
        }
        for (int i = 0; i < samples; i++) {
            double progress = samples == 1 ? 0.0 : (double) i / (samples - 1);
            double t = from + (to - from) * progress;
            ParticleFunction.Vector point = function.apply(t);
            spawnParticle(particle.at(
                    origin.x() + point.x(),
                    origin.y() + point.y(),
                    origin.z() + point.z()));
        }
    }
}
