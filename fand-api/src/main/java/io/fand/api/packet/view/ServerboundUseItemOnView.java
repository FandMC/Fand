package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundUseItemOnPacket}. */
public interface ServerboundUseItemOnView extends PacketView {

    default Object blockHit() {
        return require("blockHit", Object.class);
    }
    default Object hand() {
        return require("hand", Object.class);
    }
    default int sequence() {
        return require("sequence", int.class);
    }

    /** Returns a copy with {@code blockHit} replaced. */
    default ServerboundUseItemOnView withBlockHit(Object blockHit) {
        return (ServerboundUseItemOnView) with("blockHit", blockHit);
    }
    /** Returns a copy with {@code hand} replaced. */
    default ServerboundUseItemOnView withHand(Object hand) {
        return (ServerboundUseItemOnView) with("hand", hand);
    }
    /** Returns a copy with {@code sequence} replaced. */
    default ServerboundUseItemOnView withSequence(int sequence) {
        return (ServerboundUseItemOnView) with("sequence", sequence);
    }
}
