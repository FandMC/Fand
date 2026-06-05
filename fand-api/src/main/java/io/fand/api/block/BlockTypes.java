package io.fand.api.block;

import io.fand.api.Fand;
import java.util.NoSuchElementException;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Convenience accessor for {@link BlockType} lookups. Resolves through the
 * currently bound {@link Fand#server()}.
 */
public final class BlockTypes {

    private BlockTypes() {}

    public static Optional<? extends BlockType> find(Key key) {
        return Fand.server().blockType(key);
    }

    public static BlockType of(Key key) {
        return find(key).orElseThrow(() -> new NoSuchElementException("Unknown block type: " + key.asString()));
    }

    public static BlockType of(String key) {
        return of(Key.key(key));
    }
}
