package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSwingPacket}. */
public interface ServerboundSwingView extends PacketView {

    default Object hand() {
        return require("hand", Object.class);
    }

    /** Returns a copy with {@code hand} replaced. */
    default ServerboundSwingView withHand(Object hand) {
        return (ServerboundSwingView) with("hand", hand);
    }
}
