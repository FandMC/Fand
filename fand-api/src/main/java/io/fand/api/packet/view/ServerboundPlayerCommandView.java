package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundPlayerCommandPacket}. */
public interface ServerboundPlayerCommandView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default Object action() {
        return require("action", Object.class);
    }
    default int data() {
        return require("data", int.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ServerboundPlayerCommandView withId(int id) {
        return (ServerboundPlayerCommandView) with("id", id);
    }
    /** Returns a copy with {@code action} replaced. */
    default ServerboundPlayerCommandView withAction(Object action) {
        return (ServerboundPlayerCommandView) with("action", action);
    }
    /** Returns a copy with {@code data} replaced. */
    default ServerboundPlayerCommandView withData(int data) {
        return (ServerboundPlayerCommandView) with("data", data);
    }
}
