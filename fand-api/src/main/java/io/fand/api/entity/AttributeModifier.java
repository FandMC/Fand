package io.fand.api.entity;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/** A runtime modifier applied to one living-entity attribute. */
public record AttributeModifier(Key key, double amount, AttributeModifierOperation operation, boolean persistent) {

    public AttributeModifier {
        key = Objects.requireNonNull(key, "key");
        operation = Objects.requireNonNull(operation, "operation");
    }

    public static AttributeModifier transientModifier(Key key, double amount, AttributeModifierOperation operation) {
        return new AttributeModifier(key, amount, operation, false);
    }

    public static AttributeModifier persistentModifier(Key key, double amount, AttributeModifierOperation operation) {
        return new AttributeModifier(key, amount, operation, true);
    }
}
