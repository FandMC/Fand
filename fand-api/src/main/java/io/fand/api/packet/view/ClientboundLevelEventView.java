package io.fand.api.packet.view;

import io.fand.api.packet.BlockPosition;
import io.fand.api.packet.PacketView;

/** Typed view of a world event (block break fx, door sounds, etc.). Read-only. */
public interface ClientboundLevelEventView extends PacketView {

    /** The numeric event id. */
    default int eventType() {
        return require("type", Integer.class);
    }

    default BlockPosition position() {
        return require("pos", BlockPosition.class);
    }

    /** Event-specific data payload. */
    default int data() {
        return require("data", Integer.class);
    }

    default boolean global() {
        return require("globalEvent", Boolean.class);
    }
}
