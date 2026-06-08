package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundJigsawGeneratePacket}. */
public interface ServerboundJigsawGenerateView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default int levels() {
        return require("levels", int.class);
    }
    default boolean keepJigsaws() {
        return require("keepJigsaws", boolean.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ServerboundJigsawGenerateView withPos(Object pos) {
        return (ServerboundJigsawGenerateView) with("pos", pos);
    }
    /** Returns a copy with {@code levels} replaced. */
    default ServerboundJigsawGenerateView withLevels(int levels) {
        return (ServerboundJigsawGenerateView) with("levels", levels);
    }
    /** Returns a copy with {@code keepJigsaws} replaced. */
    default ServerboundJigsawGenerateView withKeepJigsaws(boolean keepJigsaws) {
        return (ServerboundJigsawGenerateView) with("keepJigsaws", keepJigsaws);
    }
}
