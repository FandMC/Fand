package io.fand.api.item.component;

import java.util.Locale;

/** Vanilla firework explosion shapes. */
public enum ItemFireworkShape {
    SMALL_BALL,
    LARGE_BALL,
    STAR,
    CREEPER,
    BURST;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ItemFireworkShape fromSerializedName(String value) {
        return ItemFireworkShape.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
