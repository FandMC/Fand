package io.fand.api.entity;

import net.kyori.adventure.key.Key;

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
}
