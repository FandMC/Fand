package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/** Fired during the server-side lifecycle of a living entity using an item. */
public final class EntityUseItemEvent implements Event, Cancellable {

    public enum Phase {
        START,
        TICK,
        RELEASE,
        FINISH,
        STOP
    }

    public enum Hand {
        MAIN_HAND,
        OFF_HAND
    }

    private final LivingEntity entity;
    private final ItemStack item;
    private final Hand hand;
    private final Phase phase;
    private final int usedTicks;
    private final int remainingTicks;
    private boolean cancelled;

    public EntityUseItemEvent(
            LivingEntity entity,
            ItemStack item,
            Hand hand,
            Phase phase,
            int usedTicks,
            int remainingTicks
    ) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.item = Objects.requireNonNull(item, "item");
        this.hand = Objects.requireNonNull(hand, "hand");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.usedTicks = Math.max(0, usedTicks);
        this.remainingTicks = Math.max(0, remainingTicks);
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

    public Phase phase() {
        return phase;
    }

    public int usedTicks() {
        return usedTicks;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    /** Cancellation is ignored for the observational {@link Phase#STOP} phase. */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
