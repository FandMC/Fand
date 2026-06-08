package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundDebugEventPacket}. */
public interface ClientboundDebugEventView extends PacketView {

    default Object event() {
        return require("event", Object.class);
    }

    /** Returns a copy with {@code event} replaced. */
    default ClientboundDebugEventView withEvent(Object event) {
        return (ClientboundDebugEventView) with("event", event);
    }
}
