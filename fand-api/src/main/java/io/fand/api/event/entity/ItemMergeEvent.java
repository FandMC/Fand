package io.fand.api.event.entity;

import io.fand.api.entity.ItemEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before one dropped item stack merges into another.
 */
public final class ItemMergeEvent implements Event, Cancellable {

    private final ItemEntity target;
    private final ItemEntity source;
    private final ItemStack targetItem;
    private final ItemStack sourceItem;
    private boolean cancelled;

    public ItemMergeEvent(ItemEntity target, ItemEntity source, ItemStack targetItem, ItemStack sourceItem) {
        this.target = Objects.requireNonNull(target, "target");
        this.source = Objects.requireNonNull(source, "source");
        this.targetItem = Objects.requireNonNull(targetItem, "targetItem");
        this.sourceItem = Objects.requireNonNull(sourceItem, "sourceItem");
    }

    /** Item entity that will receive the merged stack. */
    public ItemEntity target() {
        return target;
    }

    /** Item entity that will shrink or disappear. */
    public ItemEntity source() {
        return source;
    }

    public ItemStack targetItem() {
        return targetItem;
    }

    public ItemStack sourceItem() {
        return sourceItem;
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
