package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundChangeDifficultyPacket}. */
public interface ServerboundChangeDifficultyView extends PacketView {

    default Object difficulty() {
        return require("difficulty", Object.class);
    }

    /** Returns a copy with {@code difficulty} replaced. */
    default ServerboundChangeDifficultyView withDifficulty(Object difficulty) {
        return (ServerboundChangeDifficultyView) with("difficulty", difficulty);
    }
}
