package io.fand.api.entity;

import io.fand.api.Fand;
import java.util.NoSuchElementException;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Convenience accessor for {@link EntityType} lookups. Resolves through the
 * currently bound {@link Fand#server()}.
 */
public final class EntityTypes {

    private EntityTypes() {}

    public static Optional<? extends EntityType> find(Key key) {
        return Fand.server().entityType(key);
    }

    public static Optional<? extends EntityType> find(EntityKey key) {
        return find(key.key());
    }

    public static EntityType of(Key key) {
        return find(key).orElseThrow(() -> new NoSuchElementException("Unknown entity type: " + key.asString()));
    }

    public static EntityType of(EntityKey key) {
        return of(key.key());
    }

    public static EntityType of(String key) {
        return of(Key.key(key));
    }
}
