package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundPlayerActionPacket}. */
public interface ServerboundPlayerActionView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default Object direction() {
        return require("direction", Object.class);
    }
    default Object action() {
        return require("action", Object.class);
    }
    default int sequence() {
        return require("sequence", int.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ServerboundPlayerActionView withPos(Object pos) {
        return (ServerboundPlayerActionView) with("pos", pos);
    }
    /** Returns a copy with {@code direction} replaced. */
    default ServerboundPlayerActionView withDirection(Object direction) {
        return (ServerboundPlayerActionView) with("direction", direction);
    }
    /** Returns a copy with {@code action} replaced. */
    default ServerboundPlayerActionView withAction(Object action) {
        return (ServerboundPlayerActionView) with("action", action);
    }
    /** Returns a copy with {@code sequence} replaced. */
    default ServerboundPlayerActionView withSequence(int sequence) {
        return (ServerboundPlayerActionView) with("sequence", sequence);
    }
}
