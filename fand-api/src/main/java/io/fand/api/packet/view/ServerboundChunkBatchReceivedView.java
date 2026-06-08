package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundChunkBatchReceivedPacket}. */
public interface ServerboundChunkBatchReceivedView extends PacketView {

    default float desiredChunksPerTick() {
        return require("desiredChunksPerTick", float.class);
    }

    /** Returns a copy with {@code desiredChunksPerTick} replaced. */
    default ServerboundChunkBatchReceivedView withDesiredChunksPerTick(float desiredChunksPerTick) {
        return (ServerboundChunkBatchReceivedView) with("desiredChunksPerTick", desiredChunksPerTick);
    }
}
