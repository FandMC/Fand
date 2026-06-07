package io.fand.api.item.component;

import java.util.Locale;

/** Vanilla equipment slot values used by item components. */
public enum ItemEquipmentSlot {
    MAINHAND,
    OFFHAND,
    FEET,
    LEGS,
    CHEST,
    HEAD,
    BODY,
    SADDLE;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ItemEquipmentSlot fromSerializedName(String value) {
        return ItemEquipmentSlot.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
