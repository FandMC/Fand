package io.fand.api.packet.view;

import io.fand.api.packet.BlockPosition;
import io.fand.api.packet.PacketView;

/** Typed view of a block action: starting/finishing a dig, dropping items, etc. Read-only. */
public interface ServerboundPlayerActionView extends PacketView {

    /**
     * The action, e.g. {@code "START_DESTROY_BLOCK"}, {@code "STOP_DESTROY_BLOCK"},
     * {@code "DROP_ITEM"}, {@code "RELEASE_USE_ITEM"}.
     */
    default String action() {
        return require("action", String.class);
    }

    /** The targeted block position. */
    default BlockPosition position() {
        return require("pos", BlockPosition.class);
    }

    /** The targeted block face, e.g. {@code "UP"}, {@code "NORTH"}. */
    default String direction() {
        return require("direction", String.class);
    }

    /** The client prediction sequence number. */
    default int sequence() {
        return require("sequence", Integer.class);
    }
}
