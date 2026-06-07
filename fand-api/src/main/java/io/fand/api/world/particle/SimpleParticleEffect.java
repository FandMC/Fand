package io.fand.api.world.particle;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/** A particle with no particle-specific payload. */
public record SimpleParticleEffect(Key type) implements ParticleEffect {

    public SimpleParticleEffect {
        Objects.requireNonNull(type, "type");
    }
}
