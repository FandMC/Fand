package io.fand.api.entity;

import io.fand.api.command.CommandSender;
import io.fand.api.permission.PermissionSubject;
import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * A player connected to the server. Instances are thin handles backed by the
 * vanilla player object: equality is by {@link #uniqueId()} and a handle may
 * become {@linkplain #online() offline} after the player disconnects.
 */
public interface Player extends CommandSender, PermissionSubject {

    UUID uniqueId();

    /** Whether the player is still connected. */
    boolean online();

    /** Disconnects the player with the given reason. No-op if already offline. */
    void kick(Component reason);
}
