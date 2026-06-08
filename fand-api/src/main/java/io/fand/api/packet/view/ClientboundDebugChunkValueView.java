package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundDebugChunkValuePacket}. */
public interface ClientboundDebugChunkValueView extends PacketView {

    default Object chunkPos() {
        return require("chunkPos", Object.class);
    }
    default Object update() {
        return require("update", Object.class);
    }

    /** Returns a copy with {@code chunkPos} replaced. */
    default ClientboundDebugChunkValueView withChunkPos(Object chunkPos) {
        return (ClientboundDebugChunkValueView) with("chunkPos", chunkPos);
    }
    /** Returns a copy with {@code update} replaced. */
    default ClientboundDebugChunkValueView withUpdate(Object update) {
        return (ClientboundDebugChunkValueView) with("update", update);
    }
}
