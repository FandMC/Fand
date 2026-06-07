package io.fand.api.item.component;

import java.util.Locale;

/** Vanilla equipment slot groups used by attribute modifiers. */
public enum ItemEquipmentSlotGroup {
    ANY,
    MAINHAND,
    OFFHAND,
    HAND,
    FEET,
    LEGS,
    CHEST,
    HEAD,
    ARMOR,
    BODY,
    SADDLE;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ItemEquipmentSlotGroup fromSerializedName(String value) {
        return ItemEquipmentSlotGroup.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
