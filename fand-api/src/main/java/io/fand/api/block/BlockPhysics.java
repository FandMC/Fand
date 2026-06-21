package io.fand.api.block;

/**
 * Physical and behavioural flags for a block state.
 *
 * <p>{@link BlockType#physics()} describes the type's default state, while
 * {@link Block#physics()} describes the live state at a world position.
 */
public record BlockPhysics(
        float hardness,
        float blastResistance,
        int lightEmission,
        int lightLimit,
        float friction,
        float speedFactor,
        float jumpFactor,
        boolean solid,
        boolean fluid,
        boolean replaceable,
        boolean flammable,
        boolean air,
        boolean requiresTool,
        boolean hasBlockEntity,
        boolean canSurvive,
        boolean redstoneConductor
) {

    public float destroySpeed() {
        return hardness;
    }
}
