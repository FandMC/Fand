package io.fand.api.world.particle;

import io.fand.api.world.Location;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Trail particle data with target position, RGB color, and duration. */
public record TrailParticleEffect(Location target, ParticleColor color, int duration) implements ParticleEffect {

    private static final Key TYPE = Key.key("minecraft:trail");

    public TrailParticleEffect {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(color, "color");
        if (duration <= 0) {
            throw new IllegalArgumentException("duration must be > 0");
        }
    }

    @Override
    public Key type() {
        return TYPE;
    }
}
