package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Location;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before a lingering potion creates its area effect
 * cloud.
 */
public final class LingeringPotionSplashEvent implements Event, Cancellable {

    private final Entity potion;
    private final ItemStack item;
    private final Location location;
    private final @Nullable Entity source;
    private boolean cancelled;

    public LingeringPotionSplashEvent(Entity potion, ItemStack item, Location location, @Nullable Entity source) {
        this.potion = Objects.requireNonNull(potion, "potion");
        this.item = Objects.requireNonNull(item, "item");
        this.location = Objects.requireNonNull(location, "location");
        this.source = source;
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
        return Optional.ofNullable(source);
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
