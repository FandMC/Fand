package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundUseItemPacket}. */
public interface ServerboundUseItemView extends PacketView {

    default Object hand() {
        return require("hand", Object.class);
    }
    default int sequence() {
        return require("sequence", int.class);
    }
    default float yRot() {
        return require("yRot", float.class);
    }
    default float xRot() {
        return require("xRot", float.class);
    }

    /** Returns a copy with {@code hand} replaced. */
    default ServerboundUseItemView withHand(Object hand) {
        return (ServerboundUseItemView) with("hand", hand);
    }
    /** Returns a copy with {@code sequence} replaced. */
    default ServerboundUseItemView withSequence(int sequence) {
        return (ServerboundUseItemView) with("sequence", sequence);
    }
    /** Returns a copy with {@code yRot} replaced. */
    default ServerboundUseItemView withYRot(float yRot) {
        return (ServerboundUseItemView) with("yRot", yRot);
    }
    /** Returns a copy with {@code xRot} replaced. */
    default ServerboundUseItemView withXRot(float xRot) {
        return (ServerboundUseItemView) with("xRot", xRot);
    }
}
