package io.fand.api.packet;

import io.fand.api.tablist.TabListEntry;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * API-safe factory for player-info packet views.
 */
public interface PlayerInfoPacketFactory {

    PacketView add(Collection<? extends TabListEntry> entries);

    PacketView update(Collection<? extends TabListEntry> entries);

    PacketView remove(Collection<UUID> entryIds);

    default PacketView latency(UUID entryId, int latency) {
        Objects.requireNonNull(entryId, "entryId");
        return update(List.of(TabListEntry.builder(entryId, entryId.toString()).latency(latency).build()));
    }

    static PlayerInfoPacketFactory unsupported() {
        return new PlayerInfoPacketFactory() {
            @Override
            public PacketView add(Collection<? extends TabListEntry> entries) {
                throw new UnsupportedOperationException("Player-info packet factories are not supported");
            }

            @Override
            public PacketView update(Collection<? extends TabListEntry> entries) {
                throw new UnsupportedOperationException("Player-info packet factories are not supported");
            }

            @Override
            public PacketView remove(Collection<UUID> entryIds) {
                throw new UnsupportedOperationException("Player-info packet factories are not supported");
            }
        };
    }
}
