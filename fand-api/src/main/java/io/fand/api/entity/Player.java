package io.fand.api.entity;

import io.fand.api.command.CommandSender;
import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.api.permission.PermissionSubject;
import io.fand.api.scoreboard.Sidebar;
import io.fand.api.world.Location;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;

/**
 * A player connected to the server. Instances are thin handles backed by the
 * vanilla player object: equality is by {@link #uniqueId()} and a handle may
 * become {@linkplain #online() offline} after the player disconnects.
 *
 * <p>An offline player handle remains valid as a reference but read methods
 * return their last-known values. {@link #alive()} returns {@code false} once
 * the player disconnects.
 */
public interface Player extends LivingEntity, CommandSender, PermissionSubject {

    /** Whether the player is still connected. */
    boolean online();

    /** Disconnects the player with the given reason. No-op if already offline. */
    void kick(Component reason);

    /** Sends a tab-list header and footer to this player. */
    void sendTabList(Component header, Component footer);

    /** Clears the tab-list header and footer previously sent to this player. */
    void clearTabList();

    /** Shows or replaces this player's transient sidebar scoreboard. */
    void showSidebar(Sidebar sidebar);

    /** Clears the transient sidebar scoreboard sent through Fand. */
    void clearSidebar();

    /**
     * Teleports the player to {@code destination}. Schedules the move on the
     * main thread; the returned future completes with {@code true} on success
     * or {@code false} if the player went offline before the teleport ran.
     */
    CompletableFuture<Boolean> teleport(Location destination);

    /** The player's main inventory + hotbar. */
    io.fand.api.inventory.PlayerInventory inventory();

    /** The player's current game mode. */
    GameMode gameMode();

    /**
     * Switches the player to {@code mode}. Marshals to the main thread when
     * called from elsewhere; takes effect on the next tick at the latest.
     */
    void setGameMode(GameMode mode);

    /** Current food level (0-20). */
    int foodLevel();

    /**
     * Sets the food level, clamped to {@code [0, 20]}. Marshals to the main
     * thread when called from elsewhere.
     */
    void setFoodLevel(int level);

    /** Current saturation. */
    float saturation();

    /**
     * Sets saturation, clamped to {@code [0, foodLevel()]} by vanilla on the
     * next eat. Marshals to the main thread when called from elsewhere.
     */
    void setSaturation(float saturation);

    /** The XP level shown in the action bar. */
    int experienceLevel();

    /** Sets the XP level. Marshals to the main thread. */
    void setExperienceLevel(int level);

    /** Progress toward the next level, in {@code [0, 1)}. */
    float experienceProgress();

    /**
     * Sets the in-bar progress toward the next level. Values are clamped to
     * {@code [0, 1)}; vanilla rejects exact 1.0. Marshals to the main thread.
     */
    void setExperienceProgress(float progress);

    /** Awards {@code points} XP, possibly leveling the player up. */
    void giveExperience(int points);

    /** Whether the player is currently flying. */
    boolean flying();

    /**
     * Toggles flight. Has no effect (and resyncs to the client) when
     * {@link #allowFlight()} is {@code false}. Marshals to the main thread.
     */
    void setFlying(boolean flying);

    /** Whether the player is allowed to fly. */
    boolean allowFlight();

    /**
     * Sets whether flight is permitted. Disabling flight while the player is
     * already flying forces them to drop. Marshals to the main thread.
     */
    void setAllowFlight(boolean allow);

    /**
     * Opens a transient container of the given {@code type} for this player,
     * backed by an empty server-side inventory. The future completes on the
     * main thread once the menu is shown.
     *
     * <p>{@code size} is honoured for variable-size types (CHEST: 9-54 in
     * multiples of 9, HOPPER: ignored, etc.) and ignored for fixed-size
     * types. Pass {@code 0} to use the type's default size.
     *
     * <p>Returns {@link Optional#empty()} when the player is offline, the
     * type is {@link InventoryType#PLAYER} or {@link InventoryType#UNKNOWN},
     * the menu type isn't supported by this server, or an
     * {@link io.fand.api.event.inventory.InventoryOpenEvent} listener
     * cancelled the open.
     *
     * @throws IllegalArgumentException if {@code size} is invalid for the
     *         requested {@code type} (e.g. not a multiple of 9 for CHEST)
     */
    CompletableFuture<Optional<Inventory>> openInventory(InventoryType type, int size);

    /**
     * Convenience overload using the type's default size — equivalent to
     * {@code openInventory(type, 0)}.
     */
    default CompletableFuture<Optional<Inventory>> openInventory(InventoryType type) {
        return openInventory(type, 0);
    }

    /**
     * Shows {@code inventory} to this player. The inventory must be one
     * created via {@link io.fand.api.inventory.Inventories#create} — passing
     * a {@link io.fand.api.inventory.PlayerInventory} or an inventory
     * surfaced through events is rejected.
     *
     * <p>The future completes with {@code true} once the menu is shown, or
     * {@code false} when the player is offline, the inventory isn't
     * shareable, or an open listener cancelled.
     */
    CompletableFuture<Boolean> openInventory(Inventory inventory);

    /**
     * The container the player is currently viewing, if any. Returns
     * {@link Optional#empty()} when the player only has their own inventory
     * open. The returned handle reflects live state and may become stale if
     * the player closes the menu.
     */
    Optional<Inventory> openInventory();

    /**
     * Closes whatever container the player has open and returns them to
     * their own inventory. No-op if no container is open. Marshals to the
     * main thread.
     */
    void closeInventory();
}
