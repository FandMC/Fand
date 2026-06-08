package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundLoginDisconnectPacket}. */
public interface ClientboundLoginDisconnectView extends PacketView {

    default Object reason() {
        return require("reason", Object.class);
    }

    /** Returns a copy with {@code reason} replaced. */
    default ClientboundLoginDisconnectView withReason(Object reason) {
        return (ClientboundLoginDisconnectView) with("reason", reason);
    }
}
