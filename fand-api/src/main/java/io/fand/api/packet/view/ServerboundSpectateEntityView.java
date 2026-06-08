package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSpectateEntityPacket}. */
public interface ServerboundSpectateEntityView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ServerboundSpectateEntityView withEntityId(int entityId) {
        return (ServerboundSpectateEntityView) with("entityId", entityId);
    }
}
