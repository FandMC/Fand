package io.fand.api.item.component;

import java.util.Locale;

/** Vanilla dye color values. */
public enum ItemDyeColor {
    WHITE,
    ORANGE,
    MAGENTA,
    LIGHT_BLUE,
    YELLOW,
    LIME,
    PINK,
    GRAY,
    LIGHT_GRAY,
    CYAN,
    PURPLE,
    BLUE,
    BROWN,
    GREEN,
    RED,
    BLACK;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ItemDyeColor fromSerializedName(String value) {
        return ItemDyeColor.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
