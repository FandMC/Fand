package io.fand.api.world.particle;

import net.kyori.adventure.key.Key;

/** Sculk charge particle data. */
public record SculkChargeParticleEffect(float roll) implements ParticleEffect {

    private static final Key TYPE = Key.key("minecraft:sculk_charge");

    public SculkChargeParticleEffect {
        if (!Float.isFinite(roll)) {
            throw new IllegalArgumentException("roll must be finite");
        }
    }

    @Override
    public Key type() {
        return TYPE;
    }
}
