package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a bow-like weapon launches one projectile.
 */
public final class EntityShootBowEvent implements Event, Cancellable {

    private final LivingEntity shooter;
    private final ItemStack bow;
    private final ItemStack consumable;
    private final Entity projectile;
    private final float force;
    private boolean cancelled;

    public EntityShootBowEvent(LivingEntity shooter, ItemStack bow, ItemStack consumable, Entity projectile, float force) {
        this.shooter = Objects.requireNonNull(shooter, "shooter");
        this.bow = Objects.requireNonNull(bow, "bow");
        this.consumable = Objects.requireNonNull(consumable, "consumable");
        this.projectile = Objects.requireNonNull(projectile, "projectile");
        this.force = Math.max(0.0F, force);
    }

    public LivingEntity shooter() {
        return shooter;
    }

    public ItemStack bow() {
        return bow;
    }

    public ItemStack consumable() {
        return consumable;
    }

    public Entity projectile() {
        return projectile;
    }

    public float force() {
        return force;
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
