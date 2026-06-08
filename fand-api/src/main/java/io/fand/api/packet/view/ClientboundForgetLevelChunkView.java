package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundForgetLevelChunkPacket}. */
public interface ClientboundForgetLevelChunkView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ClientboundForgetLevelChunkView withPos(Object pos) {
        return (ClientboundForgetLevelChunkView) with("pos", pos);
    }
}
