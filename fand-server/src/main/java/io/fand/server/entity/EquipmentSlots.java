package io.fand.server.entity;

import io.fand.api.item.component.ItemEquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot;

final class EquipmentSlots {

    private EquipmentSlots() {
    }

    static EquipmentSlot toVanilla(ItemEquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> EquipmentSlot.MAINHAND;
            case OFFHAND -> EquipmentSlot.OFFHAND;
            case FEET -> EquipmentSlot.FEET;
            case LEGS -> EquipmentSlot.LEGS;
            case CHEST -> EquipmentSlot.CHEST;
            case HEAD -> EquipmentSlot.HEAD;
            case BODY -> EquipmentSlot.BODY;
            case SADDLE -> EquipmentSlot.SADDLE;
        };
    }
}
