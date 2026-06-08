package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Location;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a lingering potion creates its area effect
 * cloud.
 */
public final class LingeringPotionSplashEvent implements Event, Cancellable {

    private final Entity potion;
    private final ItemStack item;
    private final Location location;
    private final Optional<Entity> source;
    private boolean cancelled;

    public LingeringPotionSplashEvent(Entity potion, ItemStack item, Location location, Optional<Entity> source) {
        this.potion = Objects.requireNonNull(potion, "potion");
        this.item = Objects.requireNonNull(item, "item");
        this.location = Objects.requireNonNull(location, "location");
        this.source = Objects.requireNonNull(source, "source");
    }

    public Entity potion() {
        return potion;
    }

    public ItemStack item() {
        return item;
    }

    public Location location() {
        return location;
    }

    public Optional<Entity> source() {
        return source;
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
