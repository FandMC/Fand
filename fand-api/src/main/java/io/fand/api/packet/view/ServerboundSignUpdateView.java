package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSignUpdatePacket}. */
public interface ServerboundSignUpdateView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default Object lines() {
        return require("lines", Object.class);
    }
    default boolean isFrontText() {
        return require("isFrontText", boolean.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ServerboundSignUpdateView withPos(Object pos) {
        return (ServerboundSignUpdateView) with("pos", pos);
    }
    /** Returns a copy with {@code lines} replaced. */
    default ServerboundSignUpdateView withLines(Object lines) {
        return (ServerboundSignUpdateView) with("lines", lines);
    }
    /** Returns a copy with {@code isFrontText} replaced. */
    default ServerboundSignUpdateView withIsFrontText(boolean isFrontText) {
        return (ServerboundSignUpdateView) with("isFrontText", isFrontText);
    }
}
