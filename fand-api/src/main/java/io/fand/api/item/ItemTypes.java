package io.fand.api.item;

import io.fand.api.Fand;
import java.util.NoSuchElementException;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Convenience accessor for {@link ItemType} lookups. Resolves through the
 * currently bound {@link Fand#server()}.
 */
public final class ItemTypes {

    private ItemTypes() {}

    public static Optional<? extends ItemType> find(Key key) {
        return Fand.server().itemType(key);
    }

    public static Optional<? extends ItemType> find(ItemKey key) {
        return find(key.key());
    }

    public static ItemType of(Key key) {
        return find(key).orElseThrow(() -> new NoSuchElementException("Unknown item type: " + key.asString()));
    }

    public static ItemType of(ItemKey key) {
        return of(key.key());
    }

    public static ItemType of(String key) {
        return of(Key.key(key));
    }
}
