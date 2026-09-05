package io.fand.api.event.inventory;

import io.fand.api.block.Block;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.recipe.Recipe;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the furnace's simulation owner before it commits a smelting result.
 * Independent regions may invoke listeners concurrently.
 */
public final class FurnaceSmeltEvent extends BlockCookEvent {

    private final @Nullable Recipe recipe;

    public FurnaceSmeltEvent(Block block, Inventory inventory, @Nullable Recipe recipe, ItemStack source, ItemStack result) {
        super(block, inventory, source, result);
        this.recipe = recipe;
    }

    public Optional<Recipe> recipe() {
        return Optional.ofNullable(recipe);
    }
}
