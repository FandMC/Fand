package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSetTestBlockPacket}. */
public interface ServerboundSetTestBlockView extends PacketView {

    default Object position() {
        return require("position", Object.class);
    }
    default Object mode() {
        return require("mode", Object.class);
    }
    default String message() {
        return require("message", String.class);
    }

    /** Returns a copy with {@code position} replaced. */
    default ServerboundSetTestBlockView withPosition(Object position) {
        return (ServerboundSetTestBlockView) with("position", position);
    }
    /** Returns a copy with {@code mode} replaced. */
    default ServerboundSetTestBlockView withMode(Object mode) {
        return (ServerboundSetTestBlockView) with("mode", mode);
    }
    /** Returns a copy with {@code message} replaced. */
    default ServerboundSetTestBlockView withMessage(String message) {
        return (ServerboundSetTestBlockView) with("message", message);
    }
}
