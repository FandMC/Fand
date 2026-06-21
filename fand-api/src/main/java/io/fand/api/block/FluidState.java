package io.fand.api.block;

import io.fand.api.world.Vector3;

/** Snapshot of the fluid occupying a block position. */
public record FluidState(
        FluidType type,
        boolean source,
        boolean full,
        boolean falling,
        int amount,
        float height,
        float ownHeight,
        float blastResistance,
        Vector3 flow
) {

    public FluidState {
        java.util.Objects.requireNonNull(type, "type");
        java.util.Objects.requireNonNull(flow, "flow");
    }

    public boolean empty() {
        return type.empty();
    }

    public boolean water() {
        return type.water();
    }

    public boolean lava() {
        return type.lava();
    }

    public boolean flowing() {
        return !empty() && !source;
    }

    public static FluidState none() {
        return new FluidState(FluidTypes.EMPTY, false, false, false, 0, 0.0F, 0.0F, 0.0F, Vector3.ZERO);
    }
}
