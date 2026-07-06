package io.fand.server.inventory;

import io.fand.api.inventory.InventoryType;
import io.fand.api.inventory.PlayerInventory;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import java.util.Objects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;

public final class FandPlayerInventory implements PlayerInventory {

    private final Inventory handle;

    public FandPlayerInventory(Inventory handle) {
        this.handle = handle;
    }

    @Override
    public InventoryType type() {
        return InventoryType.PLAYER;
    }

    @Override
    public int size() {
        return handle.getContainerSize();
    }

    @Override
    public ItemStack get(int slot) {
        return FandItemStacks.fromVanilla(handle.getItem(slot));
    }

    @Override
    public void set(int slot, ItemStack stack) {
        handle.setItem(slot, FandItemStacks.toVanilla(stack));
    }

    @Override
    public ItemStack add(ItemStack stack) {
        if (stack == null || stack.empty()) {
            return ItemStack.EMPTY;
        }
        var vanilla = FandItemStacks.toVanilla(stack);
        handle.add(vanilla);
        return FandItemStacks.fromVanilla(vanilla);
    }

    @Override
    public void clear() {
        handle.clearContent();
    }

    @Override
    public int selectedSlot() {
        return handle.getSelectedSlot();
    }

    @Override
    public void setSelectedSlot(int slot) {
        if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("Selected hotbar slot must be in [0, 8], got " + slot);
        }
        handle.setSelectedSlot(slot);
    }

    @Override
    public ItemStack heldItem() {
        return FandItemStacks.fromVanilla(handle.getSelectedItem());
    }

    @Override
    public void setHeldItem(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        handle.setSelectedItem(FandItemStacks.toVanilla(stack));
    }

    @Override
    public ItemStack offhandItem() {
        return equipment(EquipmentSlot.OFFHAND);
    }

    @Override
    public void setOffhandItem(ItemStack stack) {
        setEquipment(EquipmentSlot.OFFHAND, stack);
    }

    @Override
    public ItemStack helmet() {
        return equipment(EquipmentSlot.HEAD);
    }

    @Override
    public void setHelmet(ItemStack stack) {
        setEquipment(EquipmentSlot.HEAD, stack);
    }

    @Override
    public ItemStack chestplate() {
        return equipment(EquipmentSlot.CHEST);
    }

    @Override
    public void setChestplate(ItemStack stack) {
        setEquipment(EquipmentSlot.CHEST, stack);
    }

    @Override
    public ItemStack leggings() {
        return equipment(EquipmentSlot.LEGS);
    }

    @Override
    public void setLeggings(ItemStack stack) {
        setEquipment(EquipmentSlot.LEGS, stack);
    }

    @Override
    public ItemStack boots() {
        return equipment(EquipmentSlot.FEET);
    }

    @Override
    public void setBoots(ItemStack stack) {
        setEquipment(EquipmentSlot.FEET, stack);
    }

    private ItemStack equipment(EquipmentSlot slot) {
        return FandItemStacks.fromVanilla(handle.player.getItemBySlot(slot));
    }

    private void setEquipment(EquipmentSlot slot, ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        handle.player.setItemSlot(slot, FandItemStacks.toVanilla(stack));
    }
}
