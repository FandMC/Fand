package io.fand.api.item.component;

import java.util.Locale;

/** Vanilla swing animation type values. */
public enum ItemSwingAnimationType {
    NONE,
    WHACK,
    STAB;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ItemSwingAnimationType fromSerializedName(String value) {
        return ItemSwingAnimationType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
