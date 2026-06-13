package io.fand.api.tag;

import io.fand.api.Fand;
import io.fand.api.block.BlockTagKey;
import io.fand.api.block.BlockType;
import io.fand.api.entity.EntityType;
import io.fand.api.entity.EntityTypeTagKey;
import io.fand.api.item.ItemTagKey;
import io.fand.api.item.ItemType;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Convenience accessors for vanilla registry tags. */
public final class Tags {

    private Tags() {
    }

    public static Optional<? extends Tag<BlockType>> block(Key key) {
        return Fand.server().blockTag(key);
    }

    public static Optional<? extends Tag<BlockType>> block(BlockTagKey key) {
        return block(key.key());
    }

    public static Optional<? extends Tag<BlockType>> block(String key) {
        return block(Key.key(key));
    }

    public static Tag<BlockType> requireBlock(Key key) {
        return block(key).orElseThrow(() -> new NoSuchElementException("Unknown block tag: " + key.asString()));
    }

    public static Tag<BlockType> requireBlock(BlockTagKey key) {
        return requireBlock(key.key());
    }

    public static Tag<BlockType> requireBlock(String key) {
        return requireBlock(Key.key(key));
    }

    public static Collection<? extends Tag<BlockType>> blocks() {
        return Fand.server().blockTags();
    }

    public static Optional<? extends Tag<ItemType>> item(Key key) {
        return Fand.server().itemTag(key);
    }

    public static Optional<? extends Tag<ItemType>> item(ItemTagKey key) {
        return item(key.key());
    }

    public static Optional<? extends Tag<ItemType>> item(String key) {
        return item(Key.key(key));
    }

    public static Tag<ItemType> requireItem(Key key) {
        return item(key).orElseThrow(() -> new NoSuchElementException("Unknown item tag: " + key.asString()));
    }

    public static Tag<ItemType> requireItem(ItemTagKey key) {
        return requireItem(key.key());
    }

    public static Tag<ItemType> requireItem(String key) {
        return requireItem(Key.key(key));
    }

    public static Collection<? extends Tag<ItemType>> items() {
        return Fand.server().itemTags();
    }

    public static Optional<? extends Tag<EntityType>> entityType(Key key) {
        return Fand.server().entityTypeTag(key);
    }

    public static Optional<? extends Tag<EntityType>> entityType(EntityTypeTagKey key) {
        return entityType(key.key());
    }

    public static Optional<? extends Tag<EntityType>> entityType(String key) {
        return entityType(Key.key(key));
    }

    public static Tag<EntityType> requireEntityType(Key key) {
        return entityType(key).orElseThrow(() -> new NoSuchElementException("Unknown entity type tag: " + key.asString()));
    }

    public static Tag<EntityType> requireEntityType(EntityTypeTagKey key) {
        return requireEntityType(key.key());
    }

    public static Tag<EntityType> requireEntityType(String key) {
        return requireEntityType(Key.key(key));
    }

    public static Collection<? extends Tag<EntityType>> entityTypes() {
        return Fand.server().entityTypeTags();
    }
}
