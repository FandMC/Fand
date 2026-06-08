package io.fand.api.event.inventory;

import io.fand.api.block.Block;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.recipe.Recipe;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a furnace commits a smelting result.
 */
public final class FurnaceSmeltEvent extends BlockCookEvent {

    private final Optional<Recipe> recipe;

    public FurnaceSmeltEvent(Block block, Inventory inventory, Optional<Recipe> recipe, ItemStack source, ItemStack result) {
        super(block, inventory, source, result);
        this.recipe = Objects.requireNonNull(recipe, "recipe");
    }

    public Optional<Recipe> recipe() {
        return recipe;
    }
}
