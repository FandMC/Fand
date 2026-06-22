package io.fand.api.structure;

import java.util.Objects;

/**
 * Placement schedule for generated structure sets.
 */
public record StructureGenerationPlacement(
        int spacing,
        int separation,
        int salt,
        StructureSpreadType spreadType,
        float frequency
) {

    public StructureGenerationPlacement {
        if (spacing <= 0) {
            throw new IllegalArgumentException("spacing must be positive");
        }
        if (separation < 0) {
            throw new IllegalArgumentException("separation must be non-negative");
        }
        if (separation >= spacing) {
            throw new IllegalArgumentException("separation must be smaller than spacing");
        }
        if (frequency < 0.0F || frequency > 1.0F) {
            throw new IllegalArgumentException("frequency must be between 0 and 1");
        }
        spreadType = Objects.requireNonNull(spreadType, "spreadType");
    }

    public static StructureGenerationPlacement randomSpread(int spacing, int separation, int salt) {
        return new StructureGenerationPlacement(spacing, separation, salt, StructureSpreadType.LINEAR, 1.0F);
    }

    public StructureGenerationPlacement withFrequency(float frequency) {
        return new StructureGenerationPlacement(spacing, separation, salt, spreadType, frequency);
    }
}
