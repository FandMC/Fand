package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a projectile entity is added to the world.
 */
public final class ProjectileLaunchEvent implements Event, Cancellable {

    private final Entity projectile;
    private final Optional<Entity> shooter;
    private final ItemStack item;
    private boolean cancelled;

    public ProjectileLaunchEvent(Entity projectile, Optional<Entity> shooter, ItemStack item) {
        this.projectile = Objects.requireNonNull(projectile, "projectile");
        this.shooter = Objects.requireNonNull(shooter, "shooter");
        this.item = Objects.requireNonNull(item, "item");
    }

    public Entity projectile() {
        return projectile;
    }

    public Optional<Entity> shooter() {
        return shooter;
    }

    public ItemStack item() {
        return item;
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
