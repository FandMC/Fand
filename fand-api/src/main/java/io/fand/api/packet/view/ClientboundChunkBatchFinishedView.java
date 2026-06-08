package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundChunkBatchFinishedPacket}. */
public interface ClientboundChunkBatchFinishedView extends PacketView {

    default int batchSize() {
        return require("batchSize", int.class);
    }

    /** Returns a copy with {@code batchSize} replaced. */
    default ClientboundChunkBatchFinishedView withBatchSize(int batchSize) {
        return (ClientboundChunkBatchFinishedView) with("batchSize", batchSize);
    }
}
