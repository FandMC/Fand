package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundChangeDifficultyPacket}. */
public interface ClientboundChangeDifficultyView extends PacketView {

    default Object difficulty() {
        return require("difficulty", Object.class);
    }
    default boolean locked() {
        return require("locked", boolean.class);
    }

    /** Returns a copy with {@code difficulty} replaced. */
    default ClientboundChangeDifficultyView withDifficulty(Object difficulty) {
        return (ClientboundChangeDifficultyView) with("difficulty", difficulty);
    }
    /** Returns a copy with {@code locked} replaced. */
    default ClientboundChangeDifficultyView withLocked(boolean locked) {
        return (ClientboundChangeDifficultyView) with("locked", locked);
    }
}
