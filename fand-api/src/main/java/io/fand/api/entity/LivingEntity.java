package io.fand.api.entity;

/**
 * An entity with health (mobs, players, armor stands, etc.).
 *
 * <p>{@link #setHealth(double)} runs on the server thread; off-thread writes are
 * silently rescheduled. Setting health to {@code 0} or below kills the entity
 * via the same path as {@code minecraft:generic_kill} damage, firing the usual
 * death events.
 */
public interface LivingEntity extends Entity {

    /** Current health. {@code 0} means the entity is dead or about to die. */
    double health();

    /** Maximum health, including modifiers from attributes and effects. */
    double maxHealth();

    /** Sets the entity's current health, clamped to {@code [0, maxHealth()]}. */
    void setHealth(double health);
}
