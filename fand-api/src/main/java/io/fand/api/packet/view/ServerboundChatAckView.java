package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundChatAckPacket}. */
public interface ServerboundChatAckView extends PacketView {

    default int offset() {
        return require("offset", int.class);
    }

    /** Returns a copy with {@code offset} replaced. */
    default ServerboundChatAckView withOffset(int offset) {
        return (ServerboundChatAckView) with("offset", offset);
    }
}
