package io.fand.api.item.component;

import java.util.Locale;

/** Vanilla map post-processing modes. */
public enum ItemMapPostProcessing {
    LOCK,
    SCALE;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ItemMapPostProcessing fromSerializedName(String value) {
        return ItemMapPostProcessing.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
