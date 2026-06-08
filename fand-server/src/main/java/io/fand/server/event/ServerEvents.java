package io.fand.server.event;

import io.fand.api.event.player.PlayerLoginEvent;
import io.fand.api.event.server.ServerListPingEvent;
import io.fand.server.command.AdventureBridge;
import io.fand.server.hooks.FandHooks;
import java.net.SocketAddress;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEvents.class);

    private ServerEvents() {
    }

    public static @Nullable Component fireLogin(
            MinecraftServer server,
            SocketAddress address,
            NameAndId nameAndId,
            @Nullable Component vanillaReason
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerLoginEvent.class)) {
            return vanillaReason;
        }
        var fallback = vanillaReason == null ? Component.empty() : vanillaReason;
        net.kyori.adventure.text.Component reason;
        try {
            reason = AdventureBridge.fromVanilla(fallback, server.registryAccess());
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerLoginEvent reason conversion failed; using plain text fallback", failure);
            reason = net.kyori.adventure.text.Component.text(fallback.getString());
        }
        var event = new PlayerLoginEvent(
                nameAndId.id(),
                nameAndId.name(),
                address,
                vanillaReason == null ? PlayerLoginEvent.Result.ALLOWED : PlayerLoginEvent.Result.KICK_OTHER,
                reason);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerLoginEvent listener failed", failure);
            return vanillaReason;
        }
        if (event.result() == PlayerLoginEvent.Result.ALLOWED) {
            return null;
        }
        return AdventureBridge.toVanillaOrFallback(event.kickMessage(), fallback, server.registryAccess());
    }

    public static StatusResult fireServerListPing(
            MinecraftServer server,
            Component motd,
            ServerStatus.Players players,
            boolean hidePlayers
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(ServerListPingEvent.class)) {
            return new StatusResult(motd, players);
        }
        net.kyori.adventure.text.Component adventureMotd;
        try {
            adventureMotd = AdventureBridge.fromVanilla(motd, server.registryAccess());
        } catch (RuntimeException failure) {
            LOGGER.warn("ServerListPingEvent motd conversion failed; using plain text fallback", failure);
            adventureMotd = net.kyori.adventure.text.Component.text(motd.getString());
        }
        var event = new ServerListPingEvent(adventureMotd, players.online(), players.max(), hidePlayers);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("ServerListPingEvent listener failed", failure);
            return new StatusResult(motd, players);
        }
        var nextMotd = AdventureBridge.toVanillaOrFallback(event.motd(), motd, server.registryAccess());
        List<NameAndId> sample = event.hidePlayers() ? List.of() : players.sample();
        var nextPlayers = new ServerStatus.Players(event.maxPlayers(), event.onlinePlayers(), sample);
        return new StatusResult(nextMotd, nextPlayers);
    }

    public record StatusResult(Component motd, ServerStatus.Players players) {
    }
}
