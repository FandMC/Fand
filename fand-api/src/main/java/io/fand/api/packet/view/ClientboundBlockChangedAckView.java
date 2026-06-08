package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundBlockChangedAckPacket}. */
public interface ClientboundBlockChangedAckView extends PacketView {

    default int sequence() {
        return require("sequence", int.class);
    }

    /** Returns a copy with {@code sequence} replaced. */
    default ClientboundBlockChangedAckView withSequence(int sequence) {
        return (ClientboundBlockChangedAckView) with("sequence", sequence);
    }
}
