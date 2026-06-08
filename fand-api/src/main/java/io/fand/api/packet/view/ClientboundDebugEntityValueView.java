package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundDebugEntityValuePacket}. */
public interface ClientboundDebugEntityValueView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }
    default Object update() {
        return require("update", Object.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ClientboundDebugEntityValueView withEntityId(int entityId) {
        return (ClientboundDebugEntityValueView) with("entityId", entityId);
    }
    /** Returns a copy with {@code update} replaced. */
    default ClientboundDebugEntityValueView withUpdate(Object update) {
        return (ClientboundDebugEntityValueView) with("update", update);
    }
}
