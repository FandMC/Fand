package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of a relative entity move. Read-only (vanilla Pos/Rot/PosRot
 * subtypes). Deltas are in 1/4096-block units, as sent on the wire; use
 * {@link #hasPosition} / {@link #hasRotation} to tell which were sent.
 */
public interface ClientboundMoveEntityView extends PacketView {

    default int entityId() {
        return require("entityId", Integer.class);
    }

    /** X delta in 1/4096-block units. */
    default short deltaX() {
        return require("xa", Short.class);
    }

    /** Y delta in 1/4096-block units. */
    default short deltaY() {
        return require("ya", Short.class);
    }

    /** Z delta in 1/4096-block units. */
    default short deltaZ() {
        return require("za", Short.class);
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
