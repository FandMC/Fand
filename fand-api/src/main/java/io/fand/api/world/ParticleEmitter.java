package io.fand.api.world;

/** Target that can receive fully described particle playbacks. */
@FunctionalInterface
public interface ParticleEmitter {

    /** Spawns one particle playback on this target. */
    void spawnParticle(ParticlePlayback playback);
}
