package io.fand.server.compat.modprotocol.servux;

import io.fand.server.entity.FandPlayer;
import io.fand.server.hooks.FandHooks;
import io.fand.server.messaging.FandPluginMessaging;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.minecraft.server.level.ServerPlayer;

final class ServuxSender {

    private final FandPluginMessaging messaging;

    ServuxSender(FandPluginMessaging messaging) {
        this.messaging = Objects.requireNonNull(messaging, "messaging");
    }

    void send(ServerPlayer player, Key channel, byte[] payload) {
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer != null) {
            messaging.send(fandPlayer, channel, payload);
        }
    }
}
