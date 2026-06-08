package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Fired on the server thread before a potion effect is added, changed, or
 * removed from a living entity.
 */
public final class EntityPotionEffectEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final Key effect;
    private final Optional<Effect> oldEffect;
    private final Optional<Effect> newEffect;
    private final Optional<Entity> source;
    private final Action action;
    private boolean cancelled;

    public EntityPotionEffectEvent(
            LivingEntity entity,
            Key effect,
            Optional<Effect> oldEffect,
            Optional<Effect> newEffect,
            Optional<Entity> source,
            Action action) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.effect = Objects.requireNonNull(effect, "effect");
        this.oldEffect = Objects.requireNonNull(oldEffect, "oldEffect");
        this.newEffect = Objects.requireNonNull(newEffect, "newEffect");
        this.source = Objects.requireNonNull(source, "source");
        this.action = Objects.requireNonNull(action, "action");
    }

    public LivingEntity entity() {
        return entity;
    }

    /** Vanilla mob-effect registry key, e.g. {@code minecraft:speed}. */
    public Key effect() {
        return effect;
    }

    public Optional<Effect> oldEffect() {
        return oldEffect;
    }

    public Optional<Effect> newEffect() {
        return newEffect;
    }

    public Optional<Entity> source() {
        return source;
    }

    public Action action() {
        return action;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum Action {
        ADDED,
        CHANGED,
        REMOVED,
        CLEARED
    }

    public record Effect(int durationTicks, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
    }
}
