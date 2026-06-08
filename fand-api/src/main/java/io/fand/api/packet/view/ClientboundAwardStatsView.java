package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundAwardStatsPacket}. */
public interface ClientboundAwardStatsView extends PacketView {

    default Object stats() {
        return require("stats", Object.class);
    }

    /** Returns a copy with {@code stats} replaced. */
    default ClientboundAwardStatsView withStats(Object stats) {
        return (ClientboundAwardStatsView) with("stats", stats);
    }
}
