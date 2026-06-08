package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundMoveVehiclePacket}. */
public interface ClientboundMoveVehicleView extends PacketView {

    default Object position() {
        return require("position", Object.class);
    }
    default float yRot() {
        return require("yRot", float.class);
    }
    default float xRot() {
        return require("xRot", float.class);
    }

    /** Returns a copy with {@code position} replaced. */
    default ClientboundMoveVehicleView withPosition(Object position) {
        return (ClientboundMoveVehicleView) with("position", position);
    }
    /** Returns a copy with {@code yRot} replaced. */
    default ClientboundMoveVehicleView withYRot(float yRot) {
        return (ClientboundMoveVehicleView) with("yRot", yRot);
    }
    /** Returns a copy with {@code xRot} replaced. */
    default ClientboundMoveVehicleView withXRot(float xRot) {
        return (ClientboundMoveVehicleView) with("xRot", xRot);
    }
}
