package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundDebugBlockValuePacket}. */
public interface ClientboundDebugBlockValueView extends PacketView {

    default Object blockPos() {
        return require("blockPos", Object.class);
    }
    default Object update() {
        return require("update", Object.class);
    }

    /** Returns a copy with {@code blockPos} replaced. */
    default ClientboundDebugBlockValueView withBlockPos(Object blockPos) {
        return (ClientboundDebugBlockValueView) with("blockPos", blockPos);
    }
    /** Returns a copy with {@code update} replaced. */
    default ClientboundDebugBlockValueView withUpdate(Object update) {
        return (ClientboundDebugBlockValueView) with("update", update);
    }
}
