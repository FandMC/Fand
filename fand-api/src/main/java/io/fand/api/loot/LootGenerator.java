package io.fand.api.loot;

import io.fand.api.item.ItemStack;
import java.util.List;

@FunctionalInterface
public interface LootGenerator {

    List<ItemStack> generate(LootContext context);
}
