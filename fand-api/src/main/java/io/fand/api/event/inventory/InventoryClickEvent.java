package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread when a player clicks a slot in any container.
 * Cancelling the event suppresses the vanilla mutation; the client is
 * resynced with the authoritative server state on the next broadcast.
 *
 * <p>The {@link #slot()} index is into {@link #inventory()} — i.e. the
 * top-level container view including the player's own inventory at the
 * upper end of the index range. The index {@code -999} (vanilla's outside-
 * any-slot sentinel) is normalised to {@code -1} here.
 */
public final class InventoryClickEvent implements Event, Cancellable {

    public static final int OUTSIDE = -1;

    private final Player player;
    private final Inventory inventory;
    private final int slot;
    private final int rawSlot;
    private final int containerSlot;
    private final int playerInventorySlot;
    private final ClickType clickType;
    private final int button;
    private final ItemStack currentItem;
    private final ItemStack cursorItem;
    private boolean cancelled;

    public InventoryClickEvent(
            Player player,
            Inventory inventory,
            int slot,
            ClickType clickType,
            int button,
            ItemStack currentItem,
            ItemStack cursorItem) {
        this(player, inventory, slot, slot, -1, -1, clickType, button, currentItem, cursorItem);
    }

    public InventoryClickEvent(
            Player player,
            Inventory inventory,
            int slot,
            int rawSlot,
            int containerSlot,
            int playerInventorySlot,
            ClickType clickType,
            int button,
            ItemStack currentItem,
            ItemStack cursorItem) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.slot = slot;
        this.rawSlot = rawSlot;
        this.containerSlot = containerSlot;
        this.playerInventorySlot = playerInventorySlot;
        this.clickType = Objects.requireNonNull(clickType, "clickType");
        this.button = button;
        this.currentItem = Objects.requireNonNull(currentItem, "currentItem");
        this.cursorItem = Objects.requireNonNull(cursorItem, "cursorItem");
    }

    public Player player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    /** Slot index into {@link #inventory()}, or {@link #OUTSIDE} for clicks outside any slot. */
    public int slot() {
        return slot;
    }

    /** Raw vanilla menu slot index before the outside-click sentinel is normalised. */
    public int rawSlot() {
        return rawSlot;
    }

    /**
     * Slot index inside the clicked container itself, or {@code -1} when the
     * click targeted the player inventory area or outside the menu.
     */
    public int containerSlot() {
        return containerSlot;
    }

    /**
     * Vanilla player-inventory slot (0-40) when the click targeted the player's
     * inventory area, or {@code -1} otherwise.
     */
    public int playerInventorySlot() {
        return playerInventorySlot;
    }

    public boolean outsideClick() {
        return slot == OUTSIDE;
    }

    public boolean containerClick() {
        return containerSlot >= 0;
    }

    public boolean playerInventoryClick() {
        return playerInventorySlot >= 0;
    }

    public ClickType clickType() {
        return clickType;
    }

    /**
     * Raw button number from the click packet — meaning depends on
     * {@link #clickType()}. For {@link ClickType#SWAP} this is the hotbar
     * slot index (0-8); for primary/secondary distinctions it's already
     * baked into the {@link ClickType}.
     */
    public int button() {
        return button;
    }

    /**
     * Hotbar slot for {@link ClickType#SWAP} clicks, {@code 40} for
     * {@link ClickType#SWAP_OFFHAND}, or {@code -1} for other click types.
     */
    public int hotbarButton() {
        return clickType == ClickType.SWAP || clickType == ClickType.SWAP_OFFHAND ? button : -1;
    }

    /** Item currently in {@link #slot()} before the click resolves. */
    public ItemStack currentItem() {
        return currentItem;
    }

    /** Item carried by the cursor before the click resolves. */
    public ItemStack cursorItem() {
        return cursorItem;
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
