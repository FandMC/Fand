package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundContainerSlotStateChangedPacket}. */
public interface ServerboundContainerSlotStateChangedView extends PacketView {

    default int slotId() {
        return require("slotId", int.class);
    }
    default int containerId() {
        return require("containerId", int.class);
    }
    default boolean newState() {
        return require("newState", boolean.class);
    }

    /** Returns a copy with {@code slotId} replaced. */
    default ServerboundContainerSlotStateChangedView withSlotId(int slotId) {
        return (ServerboundContainerSlotStateChangedView) with("slotId", slotId);
    }
    /** Returns a copy with {@code containerId} replaced. */
    default ServerboundContainerSlotStateChangedView withContainerId(int containerId) {
        return (ServerboundContainerSlotStateChangedView) with("containerId", containerId);
    }
    /** Returns a copy with {@code newState} replaced. */
    default ServerboundContainerSlotStateChangedView withNewState(boolean newState) {
        return (ServerboundContainerSlotStateChangedView) with("newState", newState);
    }
}
