package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundBlockUpdatePacket}. */
public interface ClientboundBlockUpdateView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default Object blockState() {
        return require("blockState", Object.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ClientboundBlockUpdateView withPos(Object pos) {
        return (ClientboundBlockUpdateView) with("pos", pos);
    }
    /** Returns a copy with {@code blockState} replaced. */
    default ClientboundBlockUpdateView withBlockState(Object blockState) {
        return (ClientboundBlockUpdateView) with("blockState", blockState);
    }
}
