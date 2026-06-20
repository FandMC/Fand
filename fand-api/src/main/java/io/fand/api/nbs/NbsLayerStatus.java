package io.fand.api.nbs;

/** Layer lock state used by modern NBS files. */
public enum NbsLayerStatus {
    NONE(0),
    LOCKED(1),
    SOLO(2);

    private final int id;

    NbsLayerStatus(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static NbsLayerStatus fromId(int id) {
        return switch (id) {
            case 1 -> LOCKED;
            case 2 -> SOLO;
            default -> NONE;
        };
    }
}
