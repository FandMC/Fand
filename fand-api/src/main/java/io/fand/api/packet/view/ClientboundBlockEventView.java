package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundBlockEventPacket}. */
public interface ClientboundBlockEventView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default int b0() {
        return require("b0", int.class);
    }
    default int b1() {
        return require("b1", int.class);
    }
    default Object block() {
        return require("block", Object.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ClientboundBlockEventView withPos(Object pos) {
        return (ClientboundBlockEventView) with("pos", pos);
    }
    /** Returns a copy with {@code b0} replaced. */
    default ClientboundBlockEventView withB0(int b0) {
        return (ClientboundBlockEventView) with("b0", b0);
    }
    /** Returns a copy with {@code b1} replaced. */
    default ClientboundBlockEventView withB1(int b1) {
        return (ClientboundBlockEventView) with("b1", b1);
    }
    /** Returns a copy with {@code block} replaced. */
    default ClientboundBlockEventView withBlock(Object block) {
        return (ClientboundBlockEventView) with("block", block);
    }
}
