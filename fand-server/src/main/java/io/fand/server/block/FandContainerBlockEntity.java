package io.fand.server.block;

import io.fand.api.block.ContainerBlockEntity;
import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.server.inventory.FandContainerInventory;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public class FandContainerBlockEntity extends FandBlockEntity implements ContainerBlockEntity {

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
        if (type == BlockEntityTypes.CHEST || type == BlockEntityTypes.TRAPPED_CHEST || type == BlockEntityTypes.BARREL) {
            return InventoryType.CHEST;
        }
        if (type == BlockEntityTypes.DISPENSER) {
            return InventoryType.DISPENSER;
        }
        if (type == BlockEntityTypes.DROPPER) {
            return InventoryType.DROPPER;
        }
        if (type == BlockEntityTypes.FURNACE) {
            return InventoryType.FURNACE;
        }
        if (type == BlockEntityTypes.BLAST_FURNACE) {
            return InventoryType.BLAST_FURNACE;
        }
        if (type == BlockEntityTypes.SMOKER) {
            return InventoryType.SMOKER;
        }
        if (type == BlockEntityTypes.HOPPER) {
            return InventoryType.HOPPER;
        }
        if (type == BlockEntityTypes.BREWING_STAND) {
            return InventoryType.BREWING;
        }
        if (type == BlockEntityTypes.SHULKER_BOX) {
            return InventoryType.SHULKER_BOX;
        }
        if (type == BlockEntityTypes.CRAFTER) {
            return InventoryType.CRAFTER;
        }
        if (type == BlockEntityTypes.LECTERN) {
            return InventoryType.LECTERN;
        }
        return InventoryType.UNKNOWN;
    }
}
