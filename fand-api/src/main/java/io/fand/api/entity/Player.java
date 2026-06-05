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
}
