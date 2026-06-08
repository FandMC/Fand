package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundMoveVehiclePacket}. */
public interface ServerboundMoveVehicleView extends PacketView {

    default Object position() {
        return require("position", Object.class);
    }
    default float yRot() {
        return require("yRot", float.class);
    }
    default float xRot() {
        return require("xRot", float.class);
    }
    default boolean onGround() {
        return require("onGround", boolean.class);
    }

    /** Returns a copy with {@code position} replaced. */
    default ServerboundMoveVehicleView withPosition(Object position) {
        return (ServerboundMoveVehicleView) with("position", position);
    }
    /** Returns a copy with {@code yRot} replaced. */
    default ServerboundMoveVehicleView withYRot(float yRot) {
        return (ServerboundMoveVehicleView) with("yRot", yRot);
    }
    /** Returns a copy with {@code xRot} replaced. */
    default ServerboundMoveVehicleView withXRot(float xRot) {
        return (ServerboundMoveVehicleView) with("xRot", xRot);
    }
    /** Returns a copy with {@code onGround} replaced. */
    default ServerboundMoveVehicleView withOnGround(boolean onGround) {
        return (ServerboundMoveVehicleView) with("onGround", onGround);
    }
}
