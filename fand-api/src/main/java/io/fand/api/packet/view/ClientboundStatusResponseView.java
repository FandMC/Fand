package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundStatusResponsePacket}. */
public interface ClientboundStatusResponseView extends PacketView {

    default Object status() {
        return require("status", Object.class);
    }

    /** Returns a copy with {@code status} replaced. */
    default ClientboundStatusResponseView withStatus(Object status) {
        return (ClientboundStatusResponseView) with("status", status);
    }
}
