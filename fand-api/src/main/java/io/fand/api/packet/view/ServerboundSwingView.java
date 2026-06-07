package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/** Typed view of a player's arm-swing animation. Read-only. */
public interface ServerboundSwingView extends PacketView {

    /** The hand swung: {@code "MAIN_HAND"} or {@code "OFF_HAND"}. */
    default String hand() {
        return require("hand", String.class);
    }
}
