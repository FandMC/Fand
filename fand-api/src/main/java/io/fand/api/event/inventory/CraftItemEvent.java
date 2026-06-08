package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.recipe.Recipe;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a player takes a crafting result.
 */
public final class CraftItemEvent implements Event, Cancellable {

    private final Player player;
    private final Inventory inventory;
    private final Optional<Recipe> recipe;
    private final ItemStack result;
    private final ClickType clickType;
    private boolean cancelled;

    public CraftItemEvent(
            Player player,
            Inventory inventory,
            Optional<Recipe> recipe,
            ItemStack result,
            ClickType clickType) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.recipe = Objects.requireNonNull(recipe, "recipe");
        this.result = Objects.requireNonNull(result, "result");
        this.clickType = Objects.requireNonNull(clickType, "clickType");
    }

    public Player player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    public Optional<Recipe> recipe() {
        return recipe;
    }

    public ItemStack result() {
        return result;
    }

    public ClickType clickType() {
        return clickType;
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
