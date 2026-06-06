package io.fand.api.item.component;

import java.util.Locale;

/** Vanilla item rarity component values. */
public enum ItemRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ItemRarity fromSerializedName(String value) {
        return ItemRarity.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
