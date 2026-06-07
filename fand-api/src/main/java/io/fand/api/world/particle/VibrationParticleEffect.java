package io.fand.api.world.particle;

import io.fand.api.world.Location;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Vibration particle data with block-position destination and arrival time. */
public record VibrationParticleEffect(Location destination, int arrivalInTicks) implements ParticleEffect {

    private static final Key TYPE = Key.key("minecraft:vibration");

    public VibrationParticleEffect {
        Objects.requireNonNull(destination, "destination");
        if (arrivalInTicks <= 0) {
            throw new IllegalArgumentException("arrivalInTicks must be > 0");
        }
    }

    @Override
    public Key type() {
        return TYPE;
    }
}
