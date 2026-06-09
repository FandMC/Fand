package io.fand.server.event;

import com.mojang.authlib.GameProfile;
import io.fand.api.event.player.AsyncPlayerPreLoginEvent;
import io.fand.api.event.player.PlayerLoginEvent;
import io.fand.api.event.player.PlayerPreLoginEvent;
import io.fand.api.event.server.ServerListPingEvent;
import io.fand.api.event.server.ServerListIcon;
import io.fand.api.event.server.ServerListVersion;
import io.fand.server.command.AdventureBridge;
import io.fand.server.hooks.FandHooks;
import io.fand.server.player.PlayerProfiles;
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

    public static LoginResult fireLogin(
            MinecraftServer server,
            SocketAddress address,
            GameProfile profile,
            @Nullable Component vanillaReason
    ) {
        var nameAndId = new NameAndId(profile);
        var bus = FandHooks.events();
        boolean hasPreLogin = bus.hasListeners(PlayerPreLoginEvent.class);
        boolean hasLogin = bus.hasListeners(PlayerLoginEvent.class);
        if (!hasPreLogin && !hasLogin) {
            return new LoginResult(profile, vanillaReason);
        }
        var fallback = vanillaReason == null ? Component.empty() : vanillaReason;
        net.kyori.adventure.text.Component reason;
        try {
            reason = AdventureBridge.fromVanilla(fallback, server.registryAccess());
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerLoginEvent reason conversion failed; using plain text fallback", failure);
            reason = net.kyori.adventure.text.Component.text(fallback.getString());
        }
        if (hasPreLogin) {
            var event = new PlayerPreLoginEvent(
                    nameAndId.id(),
                    nameAndId.name(),
                    address,
                    vanillaReason == null ? PlayerPreLoginEvent.Result.ALLOWED : PlayerPreLoginEvent.Result.KICK_OTHER,
                    reason);
            try {
                bus.fire(event);
            } catch (RuntimeException failure) {
                LOGGER.warn("PlayerPreLoginEvent listener failed", failure);
                return new LoginResult(profile, vanillaReason);
            }
            if (event.result() != PlayerPreLoginEvent.Result.ALLOWED) {
                return new LoginResult(profile, AdventureBridge.toVanillaOrFallback(event.kickMessage(), fallback, server.registryAccess()));
            }
            profile = PlayerProfiles.toGameProfile(event.profile());
            nameAndId = new NameAndId(profile);
            vanillaReason = null;
        }
        if (hasLogin) {
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
                return new LoginResult(profile, vanillaReason);
            }
            if (event.result() != PlayerLoginEvent.Result.ALLOWED) {
                return new LoginResult(profile, AdventureBridge.toVanillaOrFallback(event.kickMessage(), fallback, server.registryAccess()));
            }
            profile = PlayerProfiles.toGameProfile(event.profile());
        }
        return new LoginResult(profile, null);
    }

    public static AsyncLoginResult fireAsyncPreLogin(
            MinecraftServer server,
            SocketAddress address,
            GameProfile profile
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(AsyncPlayerPreLoginEvent.class)) {
            return new AsyncLoginResult(profile, null);
        }
        var fallback = Component.empty();
        var nameAndId = new NameAndId(profile);
        var event = new AsyncPlayerPreLoginEvent(
                nameAndId.id(),
                nameAndId.name(),
                address,
                AsyncPlayerPreLoginEvent.Result.ALLOWED,
                net.kyori.adventure.text.Component.empty());
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("AsyncPlayerPreLoginEvent listener failed", failure);
            return new AsyncLoginResult(profile, null);
        }
        if (event.result() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return new AsyncLoginResult(PlayerProfiles.toGameProfile(event.profile()), null);
        }
        return new AsyncLoginResult(
                profile,
                AdventureBridge.toVanillaOrFallback(event.kickMessage(), fallback, server.registryAccess()));
    }

    public static StatusResult fireServerListPing(
            MinecraftServer server,
            Component motd,
            ServerStatus.Players players,
            Optional<ServerStatus.Version> version,
            Optional<ServerStatus.Favicon> icon,
            boolean hidePlayers
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(ServerListPingEvent.class)) {
            return new StatusResult(motd, players, version, icon);
        }
        net.kyori.adventure.text.Component adventureMotd;
        try {
            adventureMotd = AdventureBridge.fromVanilla(motd, server.registryAccess());
        } catch (RuntimeException failure) {
            LOGGER.warn("ServerListPingEvent motd conversion failed; using plain text fallback", failure);
            adventureMotd = net.kyori.adventure.text.Component.text(motd.getString());
        }
        var event = new ServerListPingEvent(
                adventureMotd,
                players.online(),
                players.max(),
                hidePlayers,
                icon.map(value -> new ServerListIcon(value.iconBytes())).orElse(null),
                version.map(value -> new ServerListVersion(value.name(), value.protocol()))
                        .orElseGet(() -> new ServerListVersion("unknown", 0)),
                players.sample().stream().map(PlayerProfiles::fromVanilla).toList());
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("ServerListPingEvent listener failed", failure);
            return new StatusResult(motd, players, version, icon);
        }
        var nextMotd = AdventureBridge.toVanillaOrFallback(event.motd(), motd, server.registryAccess());
        List<NameAndId> sample = event.hidePlayers() ? List.of() : event.samplePlayers().stream()
                .map(PlayerProfiles::toVanilla)
                .toList();
        var nextPlayers = new ServerStatus.Players(event.maxPlayers(), event.onlinePlayers(), sample);
        var nextVersion = Optional.of(new ServerStatus.Version(event.version().name(), event.version().protocol()));
        var nextIcon = event.icon().map(value -> new ServerStatus.Favicon(value.pngBytes()));
        return new StatusResult(nextMotd, nextPlayers, nextVersion, nextIcon);
    }

    public record LoginResult(GameProfile profile, @Nullable Component reason) {
    }

    public record AsyncLoginResult(GameProfile profile, @Nullable Component reason) {
    }

    public record StatusResult(
            Component motd,
            ServerStatus.Players players,
            Optional<ServerStatus.Version> version,
            Optional<ServerStatus.Favicon> icon
    ) {
    }
}
