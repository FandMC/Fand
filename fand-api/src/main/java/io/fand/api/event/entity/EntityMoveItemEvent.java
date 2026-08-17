package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.item.component.ItemEquipmentSlot;
import java.util.Objects;

/**
 * Fired before a living entity moves items between a container and one of its
 * equipment slots. This includes Copper Golem container transport.
 */
public final class EntityMoveItemEvent implements Event, Cancellable {

    public enum Action {
        PICK_UP_FROM_CONTAINER,
        PLACE_IN_CONTAINER
    }

    private final LivingEntity entity;
    private final Inventory container;
    private final int containerSlot;
    private final ItemEquipmentSlot entitySlot;
    private final Action action;
    private ItemStack item;
    private boolean cancelled;

    public EntityMoveItemEvent(
            LivingEntity entity,
            Inventory container,
            int containerSlot,
            ItemEquipmentSlot entitySlot,
            Action action,
            ItemStack item
    ) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.container = Objects.requireNonNull(container, "container");
        if (containerSlot < 0 || containerSlot >= container.size()) {
            throw new IndexOutOfBoundsException("containerSlot: " + containerSlot);
        }
        this.containerSlot = containerSlot;
        this.entitySlot = Objects.requireNonNull(entitySlot, "entitySlot");
        this.action = Objects.requireNonNull(action, "action");
        this.item = Objects.requireNonNull(item, "item");
    }

    public LivingEntity entity() {
        return entity;
    }

    public Inventory container() {
        return container;
    }

    public int containerSlot() {
        return containerSlot;
    }

    public ItemEquipmentSlot entitySlot() {
        return entitySlot;
    }

    public Action action() {
        return action;
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
