package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundRotateHeadPacket}. */
public interface ClientboundRotateHeadView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }
    default byte yHeadRot() {
        return require("yHeadRot", byte.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ClientboundRotateHeadView withEntityId(int entityId) {
        return (ClientboundRotateHeadView) with("entityId", entityId);
    }
    /** Returns a copy with {@code yHeadRot} replaced. */
    default ClientboundRotateHeadView withYHeadRot(byte yHeadRot) {
        return (ClientboundRotateHeadView) with("yHeadRot", yHeadRot);
    }
}
