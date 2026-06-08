package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundEntityEventPacket}. */
public interface ClientboundEntityEventView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }
    default byte eventId() {
        return require("eventId", byte.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ClientboundEntityEventView withEntityId(int entityId) {
        return (ClientboundEntityEventView) with("entityId", entityId);
    }
    /** Returns a copy with {@code eventId} replaced. */
    default ClientboundEntityEventView withEventId(byte eventId) {
        return (ClientboundEntityEventView) with("eventId", eventId);
    }
}
