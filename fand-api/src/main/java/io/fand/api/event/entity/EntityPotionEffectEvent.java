package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before a potion effect is added, changed, or
 * removed from a living entity.
 */
public final class EntityPotionEffectEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final Key effect;
    private final @Nullable Effect oldEffect;
    private final @Nullable Effect newEffect;
    private final @Nullable Entity source;
    private final Action action;
    private boolean cancelled;

    public EntityPotionEffectEvent(
            LivingEntity entity,
            Key effect,
            @Nullable Effect oldEffect,
            @Nullable Effect newEffect,
            @Nullable Entity source,
            Action action) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.effect = Objects.requireNonNull(effect, "effect");
        this.oldEffect = oldEffect;
        this.newEffect = newEffect;
        this.source = source;
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
        return Optional.ofNullable(oldEffect);
    }

    public Optional<Effect> newEffect() {
        return Optional.ofNullable(newEffect);
    }

    public Optional<Entity> source() {
        return Optional.ofNullable(source);
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
