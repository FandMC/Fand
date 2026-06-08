package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundDisconnectPacket}. */
public interface ClientboundDisconnectView extends PacketView {

    default Object reason() {
        return require("reason", Object.class);
    }

    /** Returns a copy with {@code reason} replaced. */
    default ClientboundDisconnectView withReason(Object reason) {
        return (ClientboundDisconnectView) with("reason", reason);
    }
}
