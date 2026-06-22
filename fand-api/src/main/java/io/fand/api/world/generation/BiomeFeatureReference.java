package io.fand.api.world.generation;

import java.util.Objects;
import net.kyori.adventure.key.Key;

public record BiomeFeatureReference(DecorationStep step, Key placedFeature) {

    public BiomeFeatureReference {
        step = Objects.requireNonNull(step, "step");
        placedFeature = Objects.requireNonNull(placedFeature, "placedFeature");
    }
}
