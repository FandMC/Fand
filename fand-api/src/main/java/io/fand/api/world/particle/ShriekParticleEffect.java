package io.fand.api.world.particle;

import net.kyori.adventure.key.Key;

/** Sculk shriek particle data. */
public record ShriekParticleEffect(int delay) implements ParticleEffect {

    private static final Key TYPE = Key.key("minecraft:shriek");

    public ShriekParticleEffect {
        if (delay < 0) {
            throw new IllegalArgumentException("delay must be >= 0");
        }
    }

    @Override
    public Key type() {
        return TYPE;
    }
}
