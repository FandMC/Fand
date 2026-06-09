package io.fand.api.block;

import io.fand.api.item.ItemStack;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Decorated pot block entity. */
public interface DecoratedPotBlockEntity extends BlockEntity {

    ItemStack item();

    void setItem(ItemStack item);

    Optional<Key> lootTable();

    void setLootTable(Key lootTable);

    void clearLootTable();

    long lootTableSeed();

    void setLootTableSeed(long seed);

    void wobble(WobbleStyle style);

    enum WobbleStyle {
        POSITIVE,
        NEGATIVE
    }
}
