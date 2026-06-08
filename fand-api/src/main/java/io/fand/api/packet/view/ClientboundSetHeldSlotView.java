package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetHeldSlotPacket}. */
public interface ClientboundSetHeldSlotView extends PacketView {

    default int slot() {
        return require("slot", int.class);
    }

    /** Returns a copy with {@code slot} replaced. */
    default ClientboundSetHeldSlotView withSlot(int slot) {
        return (ClientboundSetHeldSlotView) with("slot", slot);
    }
}
