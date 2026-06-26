package io.fand.server.network.packet;

import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import io.fand.api.packet.PlayerInfoPacketFactory;
import io.fand.api.tablist.TabListEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class FandPlayerInfoPacketFactory implements PlayerInfoPacketFactory {

    private final PacketViewFactory views = new PacketViewFactory();

    @Override
    public PacketView add(Collection<? extends TabListEntry> entries) {
        return updateLike(List.of("ADD_PLAYER", "UPDATE_GAME_MODE", "UPDATE_LISTED", "UPDATE_LATENCY", "UPDATE_DISPLAY_NAME", "UPDATE_HAT", "UPDATE_LIST_ORDER"), entries);
    }

    @Override
    public PacketView update(Collection<? extends TabListEntry> entries) {
        return updateLike(List.of("UPDATE_GAME_MODE", "UPDATE_LISTED", "UPDATE_LATENCY", "UPDATE_DISPLAY_NAME", "UPDATE_HAT", "UPDATE_LIST_ORDER"), entries);
    }

    @Override
    public PacketView remove(Collection<UUID> entryIds) {
        Objects.requireNonNull(entryIds, "entryIds");
        return views.view(PacketType.PLAY_CLIENTBOUND_PLAYER_INFO_REMOVE, Map.of(
                "profileIds", List.copyOf(entryIds)));
    }

    private PacketView updateLike(Collection<String> actions, Collection<? extends TabListEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        return views.view(PacketType.PLAY_CLIENTBOUND_PLAYER_INFO_UPDATE, Map.of(
                "actions", List.copyOf(actions),
                "entries", List.copyOf(entries)));
    }
}
