package io.fand.api.block;

import io.fand.api.inventory.Inventory;

/**
 * Block entity that exposes item slots.
 */
public interface ContainerBlockEntity extends BlockEntity {

    Inventory inventory();
}
