package io.fand.api.entity;

import net.kyori.adventure.key.Key;
import java.util.Optional;
import java.util.Set;

/** Live handle to a living entity attribute instance. */
public interface Attribute {

    /** Attribute registry key, e.g. {@code minecraft:max_health}. */
    Key key();

    /** Current value after vanilla modifiers and effects. */
    double value();

    /** Base value before modifiers. */
    double baseValue();

    /** Sets the base value. Marshals to the server thread. */
    void setBaseValue(double value);

    /** Vanilla default value for this attribute. */
    double defaultValue();

    /** Snapshot of active transient and persistent modifiers on this instance. */
    default Set<AttributeModifier> modifiers() {
        return Set.of();
    }

    /** Active modifier by key, if present. */
    default Optional<AttributeModifier> modifier(Key key) {
        java.util.Objects.requireNonNull(key, "key");
        return modifiers().stream().filter(modifier -> modifier.key().equals(key)).findFirst();
    }

    default boolean hasModifier(Key key) {
        return modifier(key).isPresent();
    }

    /** Adds or replaces a transient modifier. Transient modifiers are not saved to disk. */
    default void addModifier(AttributeModifier modifier) {
        throw new UnsupportedOperationException("Attribute modifiers are not supported");
    }

    /** Adds or replaces a persistent modifier. Persistent modifiers are saved by vanilla player/entity data. */
    default void addPersistentModifier(Key key, double amount, AttributeModifierOperation operation) {
        addModifier(AttributeModifier.persistentModifier(key, amount, operation));
    }

    /** Adds or replaces a transient modifier. */
    default void addTransientModifier(Key key, double amount, AttributeModifierOperation operation) {
        addModifier(AttributeModifier.transientModifier(key, amount, operation));
    }

    /** Removes a modifier by key. */
    default boolean removeModifier(Key key) {
        throw new UnsupportedOperationException("Attribute modifiers are not supported");
    }
}
