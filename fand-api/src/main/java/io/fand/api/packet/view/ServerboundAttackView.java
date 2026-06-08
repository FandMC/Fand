package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundAttackPacket}. */
public interface ServerboundAttackView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ServerboundAttackView withEntityId(int entityId) {
        return (ServerboundAttackView) with("entityId", entityId);
    }
}
