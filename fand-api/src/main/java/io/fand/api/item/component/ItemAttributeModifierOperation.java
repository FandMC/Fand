package io.fand.api.item.component;

import java.util.Locale;

/** Vanilla attribute modifier operation. */
public enum ItemAttributeModifierOperation {
    ADD_VALUE,
    ADD_MULTIPLIED_BASE,
    ADD_MULTIPLIED_TOTAL;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ItemAttributeModifierOperation fromSerializedName(String value) {
        return ItemAttributeModifierOperation.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
