package io.fand.api.world.particle;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Spell particle data with RGB color and power, such as {@code effect} or {@code instant_effect}. */
public record SpellParticleEffect(Key type, ParticleColor color, float power) implements ParticleEffect {

    public SpellParticleEffect {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(color, "color");
        if (!Float.isFinite(power)) {
            throw new IllegalArgumentException("power must be finite");
        }
    }
}
