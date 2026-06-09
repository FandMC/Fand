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
 * Fired on the server thread before a smithing-table result is shown.
 */
public final class PrepareSmithingEvent implements Event {

    private final Player player;
    private final Inventory inventory;
    private final @Nullable Recipe recipe;
    private final ItemStack templateItem;
    private final ItemStack baseItem;
    private final ItemStack additionItem;
    private ItemStack result;

    public PrepareSmithingEvent(
            Player player,
            Inventory inventory,
            @Nullable Recipe recipe,
            ItemStack templateItem,
            ItemStack baseItem,
            ItemStack additionItem,
            ItemStack result) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.recipe = recipe;
        this.templateItem = Objects.requireNonNull(templateItem, "templateItem");
        this.baseItem = Objects.requireNonNull(baseItem, "baseItem");
        this.additionItem = Objects.requireNonNull(additionItem, "additionItem");
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

    public ItemStack templateItem() {
        return templateItem;
    }

    public ItemStack baseItem() {
        return baseItem;
    }

    public ItemStack additionItem() {
        return additionItem;
    }

    public ItemStack result() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = Objects.requireNonNull(result, "result");
    }
}
