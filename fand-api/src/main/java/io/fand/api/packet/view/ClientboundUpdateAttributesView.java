package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundUpdateAttributesPacket}. */
public interface ClientboundUpdateAttributesView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }
    default Object attributes() {
        return require("attributes", Object.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ClientboundUpdateAttributesView withEntityId(int entityId) {
        return (ClientboundUpdateAttributesView) with("entityId", entityId);
    }
    /** Returns a copy with {@code attributes} replaced. */
    default ClientboundUpdateAttributesView withAttributes(Object attributes) {
        return (ClientboundUpdateAttributesView) with("attributes", attributes);
    }
}
