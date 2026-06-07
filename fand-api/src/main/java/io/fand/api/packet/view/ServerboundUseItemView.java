package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/** Typed view of a player using the held item (right-click in air). Read-only. */
public interface ServerboundUseItemView extends PacketView {

    /** The hand used: {@code "MAIN_HAND"} or {@code "OFF_HAND"}. */
    default String hand() {
        return require("hand", String.class);
    }

    /** The client prediction sequence number. */
    default int sequence() {
        return require("sequence", Integer.class);
    }

    /** Yaw in degrees at the moment of use. */
    default float yaw() {
        return require("yRot", Float.class);
    }

    /** Pitch in degrees at the moment of use. */
    default float pitch() {
        return require("xRot", Float.class);
    }
}
