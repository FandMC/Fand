package io.fand.api.packet.view;

import io.fand.api.packet.BlockHit;
import io.fand.api.packet.PacketView;

/**
 * Typed view of a player using the held item on a block (right-click block).
 * Read-only.
 */
public interface ServerboundUseItemOnView extends PacketView {

    /** The hand used: {@code "MAIN_HAND"} or {@code "OFF_HAND"}. */
    default String hand() {
        return require("hand", String.class);
    }

    /** Where the player clicked: the block, the hit face, and the precise point. */
    default BlockHit hit() {
        return require("blockHit", BlockHit.class);
    }

    /** The client prediction sequence number. */
    default int sequence() {
        return require("sequence", Integer.class);
    }
}
