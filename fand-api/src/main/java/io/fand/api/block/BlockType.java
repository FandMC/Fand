package io.fand.api.block;

import io.fand.api.tag.Tag;
import io.fand.api.tag.Tags;
import java.util.Collection;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * A Minecraft block type identified by its registry key (e.g. {@code minecraft:stone}).
 *
 * <p>Types are flyweights resolved from the loaded server registry; obtain instances
 * via {@link BlockTypes#of(Key)} or the named constants on {@code BlockTypes}.
 */
public interface BlockType {

    /** Registry key, e.g. {@code minecraft:stone}. */
    Key key();

    /** Whether this block type is a member of {@code tag}. */
    default boolean is(Tag<BlockType> tag) {
        return Objects.requireNonNull(tag, "tag").contains(this);
    }

    /** Convenience overload for generated vanilla block tag keys. */
    default boolean is(BlockTagKey tag) {
        return Tags.block(tag).map(candidate -> candidate.contains(this)).orElse(false);
    }

    /** Snapshot of tags currently containing this block type. */
    default Collection<? extends Tag<BlockType>> tags() {
        return Tags.blocks().stream()
                .filter(tag -> tag.contains(this))
                .toList();
    }
}
