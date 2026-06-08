package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundCustomPayloadPacket}. */
public interface ServerboundCustomPayloadView extends PacketView {

    default Object payload() {
        return require("payload", Object.class);
    }

    /** Returns a copy with {@code payload} replaced. */
    default ServerboundCustomPayloadView withPayload(Object payload) {
        return (ServerboundCustomPayloadView) with("payload", payload);
    }
}
