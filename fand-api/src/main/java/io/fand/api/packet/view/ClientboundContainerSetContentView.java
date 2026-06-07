package io.fand.api.packet.view;

import io.fand.api.item.ItemStack;
import io.fand.api.packet.PacketView;

/**
 * Typed view of a full container content update. The slot list is exposed via
 * the dynamic {@code get("items", ...)} as an opaque value; the scalars and the
 * carried item are replaceable.
 */
public interface ClientboundContainerSetContentView extends PacketView {

    default int containerId() {
        return require("containerId", Integer.class);
    }

    default int stateId() {
        return require("stateId", Integer.class);
    }

    /** The item currently held on the cursor. */
    default ItemStack carriedItem() {
        return require("carriedItem", ItemStack.class);
    }
}
