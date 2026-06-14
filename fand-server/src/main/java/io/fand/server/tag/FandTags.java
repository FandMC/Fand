package io.fand.server.tag;

import io.fand.api.block.BlockType;
import io.fand.api.entity.EntityType;
import io.fand.api.item.ItemType;
import io.fand.api.tag.RegistryKind;
import io.fand.server.block.FandBlockType;
import io.fand.server.entity.FandEntityType;
import io.fand.server.item.FandItemType;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class FandTags {

    private FandTags() {
    }

    public static FandTag<BlockType, Block> block(HolderSet.Named<Block> handle) {
        return new FandTag<>(handle, RegistryKind.BLOCK, FandBlockType::of, FandTags::blockHolder);
    }

    public static FandTag<ItemType, Item> item(HolderSet.Named<Item> handle) {
        return new FandTag<>(handle, RegistryKind.ITEM, FandItemType::of, FandTags::itemHolder);
    }

    public static FandTag<EntityType, net.minecraft.world.entity.EntityType<?>> entityType(
            HolderSet.Named<net.minecraft.world.entity.EntityType<?>> handle
    ) {
        return new FandTag<>(handle, RegistryKind.ENTITY_TYPE, FandEntityType::of, FandTags::entityTypeHolder);
    }

    public static TagKey<Block> blockTagKey(Key key) {
        return TagKey.create(Registries.BLOCK, identifier(key));
    }

    public static TagKey<Item> itemTagKey(Key key) {
        return TagKey.create(Registries.ITEM, identifier(key));
    }

    public static TagKey<net.minecraft.world.entity.EntityType<?>> entityTypeTagKey(Key key) {
        return TagKey.create(Registries.ENTITY_TYPE, identifier(key));
    }

    public static Optional<Holder<Block>> blockHolder(BlockType type) {
        if (type instanceof FandBlockType fand) {
            return Optional.of(fand.handle().builtInRegistryHolder());
        }
        return BuiltInRegistries.BLOCK.get(identifier(type.key())).map(holder -> holder);
    }

    public static Optional<Holder<Item>> itemHolder(ItemType type) {
        if (type instanceof FandItemType fand) {
            return Optional.of(fand.handle().builtInRegistryHolder());
        }
        return BuiltInRegistries.ITEM.get(identifier(type.key())).map(holder -> holder);
    }

    public static Optional<Holder<net.minecraft.world.entity.EntityType<?>>> entityTypeHolder(EntityType type) {
        if (type instanceof FandEntityType fand) {
            return Optional.of(fand.handle().builtInRegistryHolder());
        }
        return BuiltInRegistries.ENTITY_TYPE.get(identifier(type.key())).map(holder -> holder);
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }
}
