package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundMoveMinecartPacket}. */
public interface ClientboundMoveMinecartView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }
    default Object lerpSteps() {
        return require("lerpSteps", Object.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ClientboundMoveMinecartView withEntityId(int entityId) {
        return (ClientboundMoveMinecartView) with("entityId", entityId);
    }
    /** Returns a copy with {@code lerpSteps} replaced. */
    default ClientboundMoveMinecartView withLerpSteps(Object lerpSteps) {
        return (ClientboundMoveMinecartView) with("lerpSteps", lerpSteps);
    }
}
