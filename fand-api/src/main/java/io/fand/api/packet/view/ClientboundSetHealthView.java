package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/** Typed view of a player's health/hunger update. Read-only. */
public interface ClientboundSetHealthView extends PacketView {

    default float health() {
        return require("health", Float.class);
    }

    default int food() {
        return require("food", Integer.class);
    }

    default float saturation() {
        return require("saturation", Float.class);
    }
}
