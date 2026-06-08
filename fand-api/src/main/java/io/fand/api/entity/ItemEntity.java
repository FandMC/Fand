package io.fand.api.entity;

import io.fand.api.item.ItemStack;

/**
 * Dropped item entity in the world.
 */
public interface ItemEntity extends Entity {

    ItemStack item();

    void setItem(ItemStack item);

    int age();

    boolean hasPickupDelay();

    void setPickupDelay(int ticks);

    void setNoPickupDelay();

    void setNeverPickup();

    void setUnlimitedLifetime();
}
