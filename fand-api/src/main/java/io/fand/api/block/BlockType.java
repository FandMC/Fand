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

    /** Physical attributes for this type's default block state. */
    default BlockPhysics physics() {
        return new BlockPhysics(
                0.0F,
                0.0F,
                0,
                0,
                0.6F,
                1.0F,
                1.0F,
                false,
                false,
                false,
                false,
                key().asString().equals("minecraft:air"),
                false,
                false,
                true,
                false);
    }

    default float hardness() {
        return physics().hardness();
    }

    default float destroySpeed() {
        return physics().destroySpeed();
    }

    default float blastResistance() {
        return physics().blastResistance();
    }

    default int lightEmission() {
        return physics().lightEmission();
    }

    default int lightLimit() {
        return physics().lightLimit();
    }

    default float friction() {
        return physics().friction();
    }

    default float speedFactor() {
        return physics().speedFactor();
    }

    default float jumpFactor() {
        return physics().jumpFactor();
    }

    default boolean solid() {
        return physics().solid();
    }

    default boolean fluid() {
        return physics().fluid();
    }

    default boolean replaceable() {
        return physics().replaceable();
    }

    default boolean flammable() {
        return physics().flammable();
    }

    default boolean air() {
        return physics().air();
    }

    default boolean requiresTool() {
        return physics().requiresTool();
    }

    default boolean blockEntityPresent() {
        return physics().blockEntityPresent();
    }

    default boolean canSurvive() {
        return physics().canSurvive();
    }

    default boolean redstoneConductor() {
        return physics().redstoneConductor();
    }

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
