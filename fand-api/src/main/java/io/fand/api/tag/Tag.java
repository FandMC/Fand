package io.fand.api.tag;

import java.util.Collection;
import net.kyori.adventure.key.Key;

/**
 * Read-only view of a vanilla registry tag.
 *
 * @param <T> public API type stored in the tagged registry
 */
public interface Tag<T> {

    /** Tag key without a leading {@code #}, e.g. {@code minecraft:logs}. */
    Key key();

    /** Registry this tag belongs to. */
    RegistryKind registry();

    /** Snapshot of values currently in this tag. */
    Collection<? extends T> values();

    /** Whether {@code value} is a member of this tag. */
    boolean contains(T value);

    /** Number of values currently in this tag. */
    default int size() {
        return values().size();
    }

    /** Whether this tag currently has no values. */
    default boolean empty() {
        return values().isEmpty();
    }
}
