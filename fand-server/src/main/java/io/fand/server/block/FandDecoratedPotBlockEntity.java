package io.fand.server.block;

import io.fand.api.block.DecoratedPotBlockEntity;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public final class FandDecoratedPotBlockEntity extends FandBlockEntity implements DecoratedPotBlockEntity {

    public FandDecoratedPotBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.DecoratedPotBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.DecoratedPotBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.DecoratedPotBlockEntity) handle;
    }

    @Override
    public ItemStack item() {
        return FandItemStacks.fromVanilla(handle().getTheItem());
    }

    @Override
    public void setItem(ItemStack item) {
        Objects.requireNonNull(item, "item");
        handle().setTheItem(FandItemStacks.toVanilla(item));
    }

    @Override
    public Optional<Key> lootTable() {
        var table = handle().getLootTable();
        if (table == null) {
            return Optional.empty();
        }
        var id = table.identifier();
        return Optional.of(Key.key(id.getNamespace(), id.getPath()));
    }

    @Override
    public void setLootTable(Key lootTable) {
        Objects.requireNonNull(lootTable, "lootTable");
        handle().setLootTable(ResourceKey.create(Registries.LOOT_TABLE, id(lootTable)));
    }

    @Override
    public void clearLootTable() {
        handle().setLootTable(null);
    }

    @Override
    public long lootTableSeed() {
        return handle().getLootTableSeed();
    }

    @Override
    public void setLootTableSeed(long seed) {
        handle().setLootTableSeed(seed);
    }

    @Override
    public void wobble(WobbleStyle style) {
        Objects.requireNonNull(style, "style");
        handle().wobble(switch (style) {
            case POSITIVE -> net.minecraft.world.level.block.entity.DecoratedPotBlockEntity.WobbleStyle.POSITIVE;
            case NEGATIVE -> net.minecraft.world.level.block.entity.DecoratedPotBlockEntity.WobbleStyle.NEGATIVE;
        });
    }

    private static Identifier id(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }
}
