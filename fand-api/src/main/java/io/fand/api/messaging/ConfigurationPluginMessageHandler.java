package io.fand.api.messaging;

import io.fand.api.player.PlayerProfile;

/**
 * Handles a serverbound plugin message received before the player enters the
 * play protocol.
 */
@FunctionalInterface
public interface ConfigurationPluginMessageHandler {

    void handle(PlayerProfile player, PluginMessageChannel channel, byte[] payload);
}
