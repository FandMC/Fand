package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of a player movement/look update — the core anti-cheat
 * interception point. Read-only: vanilla splits this into Pos/Rot/PosRot/
 * StatusOnly subtypes whose framing cannot be reconstructed from absolute
 * values, so it cannot be replaced (only cancelled). Use {@link #hasPosition}
 * / {@link #hasRotation} to tell which fields the client actually sent.
 */
public interface ServerboundMovePlayerView extends PacketView {

    default double x() {
        return require("x", Double.class);
    }

    default double y() {
        return require("y", Double.class);
    }

    default double z() {
        return require("z", Double.class);
    }

    /** Yaw in degrees. */
    default float yaw() {
        return require("yRot", Float.class);
    }

    /** Pitch in degrees. */
    default float pitch() {
        return require("xRot", Float.class);
    }

    default boolean onGround() {
        return require("onGround", Boolean.class);
    }

    default boolean hasPosition() {
        return require("hasPos", Boolean.class);
    }

    default boolean hasRotation() {
        return require("hasRot", Boolean.class);
    }
}
