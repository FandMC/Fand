package io.fand.api.world.particle;

import net.kyori.adventure.key.Key;

/** Type-safe description of a particle and its particle-specific data. */
public sealed interface ParticleEffect permits
        SimpleParticleEffect,
        BlockParticleEffect,
        DustParticleEffect,
        DustTransitionParticleEffect,
        ColorParticleEffect,
        SpellParticleEffect,
        PowerParticleEffect,
        SculkChargeParticleEffect,
        ShriekParticleEffect,
        TrailParticleEffect,
        VibrationParticleEffect,
        ItemParticleEffect {

    /** Vanilla particle type key. */
    Key type();
}
