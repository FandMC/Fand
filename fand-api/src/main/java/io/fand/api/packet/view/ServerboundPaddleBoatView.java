package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundPaddleBoatPacket}. */
public interface ServerboundPaddleBoatView extends PacketView {

    default boolean left() {
        return require("left", boolean.class);
    }
    default boolean right() {
        return require("right", boolean.class);
    }

    /** Returns a copy with {@code left} replaced. */
    default ServerboundPaddleBoatView withLeft(boolean left) {
        return (ServerboundPaddleBoatView) with("left", left);
    }
    /** Returns a copy with {@code right} replaced. */
    default ServerboundPaddleBoatView withRight(boolean right) {
        return (ServerboundPaddleBoatView) with("right", right);
    }
}
