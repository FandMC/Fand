package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before an entity consumes death-protection data
 * such as a totem.
 */
public final class EntityResurrectEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final ItemStack item;
    private final Hand hand;
    private boolean cancelled;

    public EntityResurrectEvent(LivingEntity entity, ItemStack item, Hand hand) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.item = Objects.requireNonNull(item, "item");
        this.hand = Objects.requireNonNull(hand, "hand");
    }

    public LivingEntity entity() {
        return entity;
    }

    public ItemStack item() {
        return item;
    }

    public Hand hand() {
        return hand;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum Hand {
        MAIN_HAND,
        OFF_HAND
    }
}
