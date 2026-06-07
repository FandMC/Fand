package io.fand.api.world.particle;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Redstone dust particle data transitioning from one RGB color to another. */
public record DustTransitionParticleEffect(ParticleColor fromColor, ParticleColor toColor, float scale)
        implements ParticleEffect {

    private static final Key TYPE = Key.key("minecraft:dust_color_transition");

    public DustTransitionParticleEffect {
        Objects.requireNonNull(fromColor, "fromColor");
        Objects.requireNonNull(toColor, "toColor");
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            throw new IllegalArgumentException("scale must be finite and > 0");
        }
    }

    @Override
    public Key type() {
        return TYPE;
    }
}
