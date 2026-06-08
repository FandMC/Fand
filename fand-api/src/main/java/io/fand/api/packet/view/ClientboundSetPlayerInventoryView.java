package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetPlayerInventoryPacket}. */
public interface ClientboundSetPlayerInventoryView extends PacketView {

    default int slot() {
        return require("slot", int.class);
    }
    default Object contents() {
        return require("contents", Object.class);
    }

    /** Returns a copy with {@code slot} replaced. */
    default ClientboundSetPlayerInventoryView withSlot(int slot) {
        return (ClientboundSetPlayerInventoryView) with("slot", slot);
    }
    /** Returns a copy with {@code contents} replaced. */
    default ClientboundSetPlayerInventoryView withContents(Object contents) {
        return (ClientboundSetPlayerInventoryView) with("contents", contents);
    }
}
