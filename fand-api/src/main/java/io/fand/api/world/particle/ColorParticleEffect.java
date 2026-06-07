package io.fand.api.world.particle;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/** A particle carrying an ARGB/RGB color payload, such as {@code entity_effect} or {@code tinted_leaves}. */
public record ColorParticleEffect(Key type, ParticleColor color) implements ParticleEffect {

    public ColorParticleEffect {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(color, "color");
    }
}
