package io.fand.api.world;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import java.util.Objects;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * Maps scanned blocks to optional batch changes.
 */
@FunctionalInterface
public interface BlockTransform {

    @Nullable BlockBatchChange apply(Block block);

    default boolean mayTransform(BlockType type) {
        Objects.requireNonNull(type, "type");
        return true;
    }

    static BlockTransform replaceMatching(Predicate<BlockType> matcher, BlockType replacement) {
        return replaceMatching(matcher, block -> true, replacement);
    }

    static BlockTransform replaceMatching(
            Predicate<BlockType> matcher,
            Predicate<Block> filter,
            BlockType replacement
    ) {
        Objects.requireNonNull(matcher, "matcher");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(replacement, "replacement");
        return new BlockTransform() {
            @Override
            public @Nullable BlockBatchChange apply(Block block) {
                return matcher.test(block.type()) && filter.test(block)
                        ? BlockBatchChange.of(block.x(), block.y(), block.z(), replacement)
                        : null;
            }

            @Override
            public boolean mayTransform(BlockType type) {
                return matcher.test(type);
            }
        };
    }
}
