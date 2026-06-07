package io.fand.api.packet.view;

import io.fand.api.item.ItemStack;
import io.fand.api.packet.PacketView;

/** Typed view of a single container slot update. Read-only. */
public interface ClientboundContainerSetSlotView extends PacketView {

    default int containerId() {
        return require("containerId", Integer.class);
    }

    default int stateId() {
        return require("stateId", Integer.class);
    }

    default int slot() {
        return require("slot", Integer.class);
    }

    /** The item placed into the slot. */
    default ItemStack item() {
        return require("itemStack", ItemStack.class);
    }
}
