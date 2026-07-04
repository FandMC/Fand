package io.fand.server.redstone;

public enum RedstoneProbeType {
    NEIGHBOR_UPDATE("neighbor-update"),
    COLLECTING_NEIGHBOR_RUN("collecting-neighbor-run"),
    SHAPE_UPDATE("shape-update"),
    WIRE_NEIGHBOR("wire-neighbor"),
    WIRE_POWER_UPDATE("wire-power-update"),
    WIRE_TARGET_STRENGTH("wire-target-strength"),
    COMPARATOR_OUTPUT("comparator-output"),
    COMPARATOR_TICK("comparator-tick"),
    DIODE_NEIGHBOR("diode-neighbor"),
    DIODE_TICK("diode-tick"),
    OBSERVER_SHAPE("observer-shape"),
    OBSERVER_TICK("observer-tick"),
    PISTON_MOVE("piston-move");

    private final String id;

    RedstoneProbeType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
