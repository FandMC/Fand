package io.fand.api.event.inventory;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.recipe.Recipe;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before a furnace starts cooking an input item.
 */
public final class FurnaceStartSmeltEvent implements Event, Cancellable {

    private final Block block;
    private final Inventory inventory;
    private final @Nullable Recipe recipe;
    private final ItemStack source;
    private int totalCookTime;
    private boolean cancelled;

    public FurnaceStartSmeltEvent(Block block, Inventory inventory, @Nullable Recipe recipe, ItemStack source, int totalCookTime) {
        this.block = Objects.requireNonNull(block, "block");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.recipe = recipe;
        this.source = Objects.requireNonNull(source, "source");
        this.totalCookTime = Math.max(0, totalCookTime);
    }

    public Block block() {
        return block;
    }

    public Inventory inventory() {
        return inventory;
    }

    public Optional<Recipe> recipe() {
        return Optional.ofNullable(recipe);
    }

    public ItemStack source() {
        return source;
    }

    public int totalCookTime() {
        return totalCookTime;
    }

    public void setTotalCookTime(int totalCookTime) {
        this.totalCookTime = Math.max(0, totalCookTime);
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
