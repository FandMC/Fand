package io.fand.api.gui;

import io.fand.api.entity.Player;
import io.fand.api.event.inventory.ClickType;
import io.fand.api.event.inventory.InventoryAction;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/** Click context passed to GUI slot handlers. */
public record GuiClick(
        GuiView view,
        Player player,
        Inventory inventory,
        int slot,
        ClickType clickType,
        InventoryAction action,
        ItemStack currentItem,
        ItemStack cursorItem
) {

    public GuiClick {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(clickType, "clickType");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(currentItem, "currentItem");
        Objects.requireNonNull(cursorItem, "cursorItem");
    }
}
