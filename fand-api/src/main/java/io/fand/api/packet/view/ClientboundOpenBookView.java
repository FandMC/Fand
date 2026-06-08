package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundOpenBookPacket}. */
public interface ClientboundOpenBookView extends PacketView {

    default Object hand() {
        return require("hand", Object.class);
    }

    /** Returns a copy with {@code hand} replaced. */
    default ClientboundOpenBookView withHand(Object hand) {
        return (ClientboundOpenBookView) with("hand", hand);
    }
}
