package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundContainerSetSlotPacket}. */
public interface ClientboundContainerSetSlotView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }
    default int stateId() {
        return require("stateId", int.class);
    }
    default int slot() {
        return require("slot", int.class);
    }
    default Object itemStack() {
        return require("itemStack", Object.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ClientboundContainerSetSlotView withContainerId(int containerId) {
        return (ClientboundContainerSetSlotView) with("containerId", containerId);
    }
    /** Returns a copy with {@code stateId} replaced. */
    default ClientboundContainerSetSlotView withStateId(int stateId) {
        return (ClientboundContainerSetSlotView) with("stateId", stateId);
    }
    /** Returns a copy with {@code slot} replaced. */
    default ClientboundContainerSetSlotView withSlot(int slot) {
        return (ClientboundContainerSetSlotView) with("slot", slot);
    }
    /** Returns a copy with {@code itemStack} replaced. */
    default ClientboundContainerSetSlotView withItemStack(Object itemStack) {
        return (ClientboundContainerSetSlotView) with("itemStack", itemStack);
    }
}
