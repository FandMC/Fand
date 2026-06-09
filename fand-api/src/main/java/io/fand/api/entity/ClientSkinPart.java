package io.fand.api.entity;

/** Toggleable skin layers reported by the client. */
public enum ClientSkinPart {
    CAPE(0),
    JACKET(1),
    LEFT_SLEEVE(2),
    RIGHT_SLEEVE(3),
    LEFT_PANTS_LEG(4),
    RIGHT_PANTS_LEG(5),
    HAT(6);

    private final int bit;
    private final int mask;

    ClientSkinPart(int bit) {
        this.bit = bit;
        this.mask = 1 << bit;
    }

    public int bit() {
        return bit;
    }

    public int mask() {
        return mask;
    }
}
