package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundLockDifficultyPacket}. */
public interface ServerboundLockDifficultyView extends PacketView {

    default boolean locked() {
        return require("locked", boolean.class);
    }

    /** Returns a copy with {@code locked} replaced. */
    default ServerboundLockDifficultyView withLocked(boolean locked) {
        return (ServerboundLockDifficultyView) with("locked", locked);
    }
}
