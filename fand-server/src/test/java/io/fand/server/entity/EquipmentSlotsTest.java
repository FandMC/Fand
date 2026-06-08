package io.fand.server.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.fand.api.item.component.ItemEquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

class EquipmentSlotsTest {

    @Test
    void mapsAllApiSlotsToVanillaSlots() {
        assertEquals(EquipmentSlot.MAINHAND, EquipmentSlots.toVanilla(ItemEquipmentSlot.MAINHAND));
        assertEquals(EquipmentSlot.OFFHAND, EquipmentSlots.toVanilla(ItemEquipmentSlot.OFFHAND));
        assertEquals(EquipmentSlot.FEET, EquipmentSlots.toVanilla(ItemEquipmentSlot.FEET));
        assertEquals(EquipmentSlot.LEGS, EquipmentSlots.toVanilla(ItemEquipmentSlot.LEGS));
        assertEquals(EquipmentSlot.CHEST, EquipmentSlots.toVanilla(ItemEquipmentSlot.CHEST));
        assertEquals(EquipmentSlot.HEAD, EquipmentSlots.toVanilla(ItemEquipmentSlot.HEAD));
        assertEquals(EquipmentSlot.BODY, EquipmentSlots.toVanilla(ItemEquipmentSlot.BODY));
        assertEquals(EquipmentSlot.SADDLE, EquipmentSlots.toVanilla(ItemEquipmentSlot.SADDLE));
    }
}
