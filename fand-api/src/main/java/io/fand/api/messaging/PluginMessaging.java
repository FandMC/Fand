package io.fand.api.messaging;

import io.fand.api.entity.Player;
import java.util.Collection;
import java.util.Collections;
import net.kyori.adventure.key.Key;

/**
 * Standard plugin messaging channel API for custom payloads.
 */
public interface PluginMessaging {

    Collection<PluginMessageChannel> channels();

    default PluginMessageRegistration register(Key channel, PluginMessageDirection direction) {
        throw new UnsupportedOperationException("Plugin messaging channels are not supported");
    }

    default PluginMessageRegistration register(Key channel, PluginMessageDirection direction, PluginMessageHandler handler) {
        throw new UnsupportedOperationException("Plugin messaging channels are not supported");
    }

    /**
     * Registers a serverbound handler for messages received during the
     * configuration protocol, before a {@link Player} exists.
     *
     * @param channel channel to advertise and receive
     * @param handler handler receiving the connecting player's stable profile
     * @return registration used to stop receiving messages
     */
    default PluginMessageRegistration registerConfiguration(
            Key channel,
            ConfigurationPluginMessageHandler handler
    ) {
        throw new UnsupportedOperationException("Configuration plugin messaging channels are not supported");
    }

    default void send(Player player, Key channel, byte[] payload) {
        throw new UnsupportedOperationException("Plugin messaging channels are not supported");
    }

    static PluginMessaging empty() {
        return new PluginMessaging() {
            @Override
            public Collection<PluginMessageChannel> channels() {
                return Collections.emptyList();
            }
        };
    }
}
