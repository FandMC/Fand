package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.recipe.Recipe;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread when vanilla recalculates a crafting result slot.
 */
public final class PrepareItemCraftEvent implements Event {

    private final Player player;
    private final Inventory inventory;
    private final @Nullable Recipe recipe;
    private ItemStack result;

    public PrepareItemCraftEvent(Player player, Inventory inventory, @Nullable Recipe recipe, ItemStack result) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.recipe = recipe;
        this.result = Objects.requireNonNull(result, "result");
    }

    public Player player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    public Optional<Recipe> recipe() {
        return Optional.ofNullable(recipe);
    }

    public ItemStack result() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = Objects.requireNonNull(result, "result");
    }
}
