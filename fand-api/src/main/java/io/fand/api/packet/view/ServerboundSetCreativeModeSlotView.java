package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSetCreativeModeSlotPacket}. */
public interface ServerboundSetCreativeModeSlotView extends PacketView {

    default short slotNum() {
        return require("slotNum", short.class);
    }
    default Object itemStack() {
        return require("itemStack", Object.class);
    }

    /** Returns a copy with {@code slotNum} replaced. */
    default ServerboundSetCreativeModeSlotView withSlotNum(short slotNum) {
        return (ServerboundSetCreativeModeSlotView) with("slotNum", slotNum);
    }
    /** Returns a copy with {@code itemStack} replaced. */
    default ServerboundSetCreativeModeSlotView withItemStack(Object itemStack) {
        return (ServerboundSetCreativeModeSlotView) with("itemStack", itemStack);
    }
}
