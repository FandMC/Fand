package io.fand.api.world.particle;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/** A particle carrying a power value, such as {@code dragon_breath}. */
public record PowerParticleEffect(Key type, float power) implements ParticleEffect {

    public PowerParticleEffect {
        Objects.requireNonNull(type, "type");
        if (!Float.isFinite(power)) {
            throw new IllegalArgumentException("power must be finite");
        }
    }
}
