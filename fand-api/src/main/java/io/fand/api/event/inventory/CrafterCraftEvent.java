package io.fand.api.event.inventory;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.recipe.Recipe;
import java.util.List;
import java.util.Objects;

/** Fired before a Crafter emits output or consumes any ingredient. */
public final class CrafterCraftEvent implements Event, Cancellable {

    private final Block crafter;
    private final Inventory inventory;
    private final Recipe recipe;
    private final List<ItemStack> input;
    private ItemStack result;
    private List<ItemStack> remainingItems;
    private boolean cancelled;

    public CrafterCraftEvent(
            Block crafter,
            Inventory inventory,
            Recipe recipe,
            List<ItemStack> input,
            ItemStack result,
            List<ItemStack> remainingItems
    ) {
        this.crafter = Objects.requireNonNull(crafter, "crafter");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.recipe = Objects.requireNonNull(recipe, "recipe");
        this.input = List.copyOf(Objects.requireNonNull(input, "input"));
        this.result = Objects.requireNonNull(result, "result");
        setRemainingItems(remainingItems);
    }

    public Block crafter() {
        return crafter;
    }

    public Inventory inventory() {
        return inventory;
    }

    public Recipe recipe() {
        return recipe;
    }

    public List<ItemStack> input() {
        return input;
    }

    public ItemStack result() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    public List<ItemStack> remainingItems() {
        return remainingItems;
    }

    public void setRemainingItems(List<ItemStack> remainingItems) {
        var copy = List.copyOf(Objects.requireNonNull(remainingItems, "remainingItems"));
        if (copy.size() != input.size()) {
            throw new IllegalArgumentException("Remaining items must have the same size as the input snapshot");
        }
        this.remainingItems = copy;
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
