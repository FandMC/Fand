package io.fand.api.item.component;

import java.util.Locale;

/** Vanilla item use animation values. */
public enum ItemUseAnimation {
    NONE,
    EAT,
    DRINK,
    BLOCK,
    BOW,
    TRIDENT,
    CROSSBOW,
    SPYGLASS,
    TOOT_HORN,
    BRUSH,
    BUNDLE,
    SPEAR;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ItemUseAnimation fromSerializedName(String value) {
        return ItemUseAnimation.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
