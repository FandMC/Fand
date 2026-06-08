package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundBlockDestructionPacket}. */
public interface ClientboundBlockDestructionView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default Object pos() {
        return require("pos", Object.class);
    }
    default int progress() {
        return require("progress", int.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundBlockDestructionView withId(int id) {
        return (ClientboundBlockDestructionView) with("id", id);
    }
    /** Returns a copy with {@code pos} replaced. */
    default ClientboundBlockDestructionView withPos(Object pos) {
        return (ClientboundBlockDestructionView) with("pos", pos);
    }
    /** Returns a copy with {@code progress} replaced. */
    default ClientboundBlockDestructionView withProgress(int progress) {
        return (ClientboundBlockDestructionView) with("progress", progress);
    }
}
