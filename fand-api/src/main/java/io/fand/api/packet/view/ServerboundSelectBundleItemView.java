package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSelectBundleItemPacket}. */
public interface ServerboundSelectBundleItemView extends PacketView {

    default int slotId() {
        return require("slotId", int.class);
    }
    default int selectedItemIndex() {
        return require("selectedItemIndex", int.class);
    }

    /** Returns a copy with {@code slotId} replaced. */
    default ServerboundSelectBundleItemView withSlotId(int slotId) {
        return (ServerboundSelectBundleItemView) with("slotId", slotId);
    }
    /** Returns a copy with {@code selectedItemIndex} replaced. */
    default ServerboundSelectBundleItemView withSelectedItemIndex(int selectedItemIndex) {
        return (ServerboundSelectBundleItemView) with("selectedItemIndex", selectedItemIndex);
    }
}
