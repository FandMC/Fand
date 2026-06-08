package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundChunksBiomesPacket}. */
public interface ClientboundChunksBiomesView extends PacketView {

    default Object chunkBiomeData() {
        return require("chunkBiomeData", Object.class);
    }

    /** Returns a copy with {@code chunkBiomeData} replaced. */
    default ClientboundChunksBiomesView withChunkBiomeData(Object chunkBiomeData) {
        return (ClientboundChunksBiomesView) with("chunkBiomeData", chunkBiomeData);
    }
}
