package io.fand.server.block;

import io.fand.api.block.ContainerBlockEntity;
import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.server.inventory.FandContainerInventory;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class FandContainerBlockEntity extends FandBlockEntity implements ContainerBlockEntity {

    private final Container container;
    private final Inventory inventory;

    public FandContainerBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.BlockEntity handle, Container container) {
        super(block, handle);
        this.container = container;
        this.inventory = new FandContainerInventory(container, inventoryType(handle));
    }

    public Container container() {
        return container;
    }

    @Override
    public Inventory inventory() {
        return inventory;
    }

    private static InventoryType inventoryType(net.minecraft.world.level.block.entity.BlockEntity handle) {
        var type = handle.getType();
        if (type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST || type == BlockEntityType.BARREL) {
            return InventoryType.CHEST;
        }
        if (type == BlockEntityType.DISPENSER) {
            return InventoryType.DISPENSER;
        }
        if (type == BlockEntityType.DROPPER) {
            return InventoryType.DROPPER;
        }
        if (type == BlockEntityType.FURNACE) {
            return InventoryType.FURNACE;
        }
        if (type == BlockEntityType.BLAST_FURNACE) {
            return InventoryType.BLAST_FURNACE;
        }
        if (type == BlockEntityType.SMOKER) {
            return InventoryType.SMOKER;
        }
        if (type == BlockEntityType.HOPPER) {
            return InventoryType.HOPPER;
        }
        if (type == BlockEntityType.BREWING_STAND) {
            return InventoryType.BREWING;
        }
        if (type == BlockEntityType.SHULKER_BOX) {
            return InventoryType.SHULKER_BOX;
        }
        if (type == BlockEntityType.CRAFTER) {
            return InventoryType.CRAFTER;
        }
        if (type == BlockEntityType.LECTERN) {
            return InventoryType.LECTERN;
        }
        return InventoryType.UNKNOWN;
    }
}
