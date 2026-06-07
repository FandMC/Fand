package io.fand.api.world.particle;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Redstone dust particle data with RGB color and scale. */
public record DustParticleEffect(ParticleColor color, float scale) implements ParticleEffect {

    private static final Key TYPE = Key.key("minecraft:dust");

    public DustParticleEffect {
        Objects.requireNonNull(color, "color");
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            throw new IllegalArgumentException("scale must be finite and > 0");
        }
    }

    @Override
    public Key type() {
        return TYPE;
    }
}
