package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundCustomPayloadPacket}. */
public interface ClientboundCustomPayloadView extends PacketView {

    default Object payload() {
        return require("payload", Object.class);
    }

    /** Returns a copy with {@code payload} replaced. */
    default ClientboundCustomPayloadView withPayload(Object payload) {
        return (ClientboundCustomPayloadView) with("payload", payload);
    }
}
