package io.fand.server.tablist;

import com.mojang.authlib.GameProfile;
import io.fand.api.entity.GameMode;
import io.fand.api.packet.PlayerInfoEntry;
import io.fand.api.tablist.TabListEntry;
import io.fand.server.command.AdventureBridge;
import io.fand.server.player.PlayerProfiles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

public final class FandTabListPackets {

    static final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> INITIALIZE_ACTIONS = EnumSet.of(
            ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER);

    static final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> UPDATE_ACTIONS = EnumSet.of(
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER);

    private FandTabListPackets() {
    }

    static ClientboundPlayerInfoUpdatePacket add(TabListEntry entry, MinecraftServer server) {
        return packet(INITIALIZE_ACTIONS, List.of(entry(entry, server)));
    }

    static ClientboundPlayerInfoUpdatePacket update(TabListEntry entry, MinecraftServer server) {
        return packet(UPDATE_ACTIONS, List.of(entry(entry, server)));
    }

    public static ClientboundPlayerInfoUpdatePacket latency(UUID entryId, int latency) {
        return packet(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY),
                List.of(new ClientboundPlayerInfoUpdatePacket.Entry(
                        entryId,
                        null,
                        true,
                        Math.max(0, latency),
                        GameType.DEFAULT_MODE,
                        null,
                        true,
                        0,
                        null)));
    }

    public static ClientboundPlayerInfoUpdatePacket packet(
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions,
            Collection<ClientboundPlayerInfoUpdatePacket.Entry> entries
    ) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(entries, "entries");
        return ClientboundPlayerInfoUpdatePacket.fromEntries(actions, entries);
    }

    public static ClientboundPlayerInfoUpdatePacket packetFromApi(
            Collection<String> actions,
            Collection<PlayerInfoEntry> entries,
            @Nullable MinecraftServer server
    ) {
        return packetFromApi(actions, entries, server, null);
    }

    public static ClientboundPlayerInfoUpdatePacket packetFromApi(
            Collection<String> actions,
            Collection<PlayerInfoEntry> entries,
            @Nullable MinecraftServer server,
            @Nullable ClientboundPlayerInfoUpdatePacket original
    ) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(entries, "entries");
        var convertedActions = EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
        for (var action : actions) {
            convertedActions.add(ClientboundPlayerInfoUpdatePacket.Action.valueOf(action));
        }
        if (convertedActions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)
                && entries.stream().anyMatch(entry -> entry.profile() == null)) {
            throw new IllegalArgumentException("ADD_PLAYER entries require a profile");
        }
        return packet(
                convertedActions,
                entries.stream()
                        .map(entry -> entry(entry, server, originalEntry(original, entry.profileId())))
                        .toList());
    }

    public static List<PlayerInfoEntry> apiEntries(
            ClientboundPlayerInfoUpdatePacket packet,
            @Nullable MinecraftServer server
    ) {
        Objects.requireNonNull(packet, "packet");
        return packet.entries().stream()
                .map(entry -> apiEntry(entry, server))
                .toList();
    }

    public static ClientboundPlayerInfoUpdatePacket rewrite(
            ClientboundPlayerInfoUpdatePacket packet,
            Set<UUID> hiddenEntries,
            Map<UUID, Integer> displayedPings
    ) {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(hiddenEntries, "hiddenEntries");
        Objects.requireNonNull(displayedPings, "displayedPings");
        if (hiddenEntries.isEmpty() && (displayedPings.isEmpty()
                || !packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY))) {
            return packet;
        }

        var rewritten = new ArrayList<ClientboundPlayerInfoUpdatePacket.Entry>(packet.entries().size());
        boolean changed = false;
        boolean rewriteLatency = packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY);
        for (var entry : packet.entries()) {
            if (hiddenEntries.contains(entry.profileId())) {
                changed = true;
                continue;
            }
            var latency = rewriteLatency ? displayedPings.get(entry.profileId()) : null;
            if (latency == null || latency == entry.latency()) {
                rewritten.add(entry);
                continue;
            }
            changed = true;
            rewritten.add(withLatency(entry, latency));
        }

        if (!changed) {
            return packet;
        }
        return packet(EnumSet.copyOf(packet.actions()), rewritten);
    }

    private static ClientboundPlayerInfoUpdatePacket.Entry entry(TabListEntry entry, @Nullable MinecraftServer server) {
        Objects.requireNonNull(entry, "entry");
        return entry(PlayerInfoEntry.from(entry), server, null);
    }

    private static ClientboundPlayerInfoUpdatePacket.Entry entry(
            PlayerInfoEntry entry,
            @Nullable MinecraftServer server,
            ClientboundPlayerInfoUpdatePacket.@Nullable Entry original
    ) {
        Objects.requireNonNull(entry, "entry");
        return new ClientboundPlayerInfoUpdatePacket.Entry(
                entry.profileId(),
                profile(entry, original),
                entry.listed(),
                entry.latency(),
                gameMode(entry.gameMode()),
                displayName(entry, server, original),
                entry.showHat(),
                entry.order(),
                original == null ? (RemoteChatSession.Data) null : original.chatSession());
    }

    private static @Nullable GameProfile profile(
            PlayerInfoEntry entry,
            ClientboundPlayerInfoUpdatePacket.@Nullable Entry original
    ) {
        var profile = entry.profile();
        if (profile == null) {
            return null;
        }
        if (original != null && original.profile() != null
                && profile.equals(PlayerProfiles.fromVanilla(original.profile()))) {
            return original.profile();
        }
        return PlayerProfiles.toGameProfile(profile);
    }

    private static GameType gameMode(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> GameType.SURVIVAL;
            case CREATIVE -> GameType.CREATIVE;
            case ADVENTURE -> GameType.ADVENTURE;
            case SPECTATOR -> GameType.SPECTATOR;
        };
    }

    private static @Nullable Component displayName(
            PlayerInfoEntry entry,
            @Nullable MinecraftServer server,
            ClientboundPlayerInfoUpdatePacket.@Nullable Entry original
    ) {
        var displayName = entry.displayName();
        if (displayName == null) {
            return null;
        }
        if (original != null && original.displayName() != null
                && displayName.equals(AdventureBridge.fromVanilla(
                        original.displayName(), server == null ? null : server.registryAccess()))) {
            return original.displayName();
        }
        if (server == null) {
            throw new IllegalStateException("Minecraft server is not attached");
        }
        return AdventureBridge.toVanilla(displayName, server.registryAccess());
    }

    private static PlayerInfoEntry apiEntry(
            ClientboundPlayerInfoUpdatePacket.Entry entry,
            @Nullable MinecraftServer server
    ) {
        return new PlayerInfoEntry(
                entry.profileId(),
                entry.profile() == null ? null : PlayerProfiles.fromVanilla(entry.profile()),
                entry.listed(),
                entry.latency(),
                gameMode(entry.gameMode()),
                entry.displayName() == null ? null : AdventureBridge.fromVanilla(
                        entry.displayName(), server == null ? null : server.registryAccess()),
                entry.showHat(),
                entry.listOrder());
    }

    private static GameMode gameMode(GameType mode) {
        return switch (mode) {
            case SURVIVAL -> GameMode.SURVIVAL;
            case CREATIVE -> GameMode.CREATIVE;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SPECTATOR -> GameMode.SPECTATOR;
        };
    }

    private static ClientboundPlayerInfoUpdatePacket.@Nullable Entry originalEntry(
            @Nullable ClientboundPlayerInfoUpdatePacket packet,
            UUID profileId
    ) {
        if (packet == null) {
            return null;
        }
        for (var entry : packet.entries()) {
            if (entry.profileId().equals(profileId)) {
                return entry;
            }
        }
        return null;
    }

    private static ClientboundPlayerInfoUpdatePacket.Entry withLatency(
            ClientboundPlayerInfoUpdatePacket.Entry entry,
            int latency
    ) {
        return new ClientboundPlayerInfoUpdatePacket.Entry(
                entry.profileId(),
                entry.profile(),
                entry.listed(),
                Math.max(0, latency),
                entry.gameMode(),
                entry.displayName(),
                entry.showHat(),
                entry.listOrder(),
                entry.chatSession());
    }
}
