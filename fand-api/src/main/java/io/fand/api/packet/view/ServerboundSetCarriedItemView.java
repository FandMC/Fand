package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSetCarriedItemPacket}. */
public interface ServerboundSetCarriedItemView extends PacketView {

    default int slot() {
        return require("slot", int.class);
    }

    /** Returns a copy with {@code slot} replaced. */
    default ServerboundSetCarriedItemView withSlot(int slot) {
        return (ServerboundSetCarriedItemView) with("slot", slot);
    }
}
