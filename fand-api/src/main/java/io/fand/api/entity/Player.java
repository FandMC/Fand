package io.fand.api.entity;

import io.fand.api.command.CommandSender;
import io.fand.api.permission.PermissionSubject;
import io.fand.api.world.Location;
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
}
