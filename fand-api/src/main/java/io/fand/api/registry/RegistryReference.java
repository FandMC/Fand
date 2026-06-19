package io.fand.api.registry;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Reference to either a concrete registry entry or a tag inside that registry.
 */
public record RegistryReference(Key key, boolean tag) {

    public RegistryReference {
        Objects.requireNonNull(key, "key");
    }

    public static RegistryReference key(Key key) {
        return new RegistryReference(key, false);
    }

    public static RegistryReference tag(Key key) {
        return new RegistryReference(key, true);
    }

    public static RegistryReference all() {
        return tag(Key.key("fand:all"));
    }

    public static RegistryReference parse(String value) {
        Objects.requireNonNull(value, "value");
        return value.startsWith("#")
                ? tag(Key.key(value.substring(1)))
                : key(Key.key(value));
    }

    public String asString() {
        return tag ? "#" + key.asString() : key.asString();
    }
}
