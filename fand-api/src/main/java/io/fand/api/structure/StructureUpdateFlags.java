package io.fand.api.structure;

public final class StructureUpdateFlags {

    public static final int NEIGHBORS = 1;
    public static final int CLIENTS = 2;
    public static final int NO_RERENDER = 4;
    public static final int RERENDER_MAIN_THREAD = 8;
    public static final int FORCE_STATE = 16;
    public static final int SKIP_DROPS = 32;
    public static final int UPDATE_MOVE_BY_PISTON = 64;
    public static final int SKIP_LIGHTING_UPDATES = 128;
    public static final int IMMEDIATE = 512;
    public static final int DEFAULT = CLIENTS | SKIP_DROPS | SKIP_LIGHTING_UPDATES;

    private StructureUpdateFlags() {
    }
}
