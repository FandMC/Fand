package io.fand.api.packet.view;

import io.fand.api.item.ItemStack;
import io.fand.api.packet.PacketView;

/** Typed view of a creative-mode inventory edit. Replaceable. */
public interface ServerboundSetCreativeModeSlotView extends PacketView {

    /** The inventory slot being set. */
    default short slot() {
        return require("slotNum", Short.class);
    }

    /** The item placed into the slot. */
    default ItemStack item() {
        return require("itemStack", ItemStack.class);
    }
}
