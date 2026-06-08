package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Location;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a splash potion applies effects to nearby
 * living entities.
 */
public final class PotionSplashEvent implements Event, Cancellable {

    private final Entity potion;
    private final ItemStack item;
    private final Location location;
    private final Optional<Entity> source;
    private final Map<LivingEntity, Double> affectedEntities;
    private boolean cancelled;

    public PotionSplashEvent(
            Entity potion,
            ItemStack item,
            Location location,
            Optional<Entity> source,
            Map<LivingEntity, Double> affectedEntities
    ) {
        this.potion = Objects.requireNonNull(potion, "potion");
        this.item = Objects.requireNonNull(item, "item");
        this.location = Objects.requireNonNull(location, "location");
        this.source = Objects.requireNonNull(source, "source");
        this.affectedEntities = new java.util.LinkedHashMap<>(Objects.requireNonNull(affectedEntities, "affectedEntities"));
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

    /**
     * Mutable map of affected entities to vanilla intensity, from 0.0 to 1.0.
     */
    public Map<LivingEntity, Double> affectedEntities() {
        return affectedEntities;
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
