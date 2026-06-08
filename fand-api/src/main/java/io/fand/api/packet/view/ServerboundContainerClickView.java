package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundContainerClickPacket}. */
public interface ServerboundContainerClickView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }
    default int stateId() {
        return require("stateId", int.class);
    }
    default short slotNum() {
        return require("slotNum", short.class);
    }
    default byte buttonNum() {
        return require("buttonNum", byte.class);
    }
    default Object containerInput() {
        return require("containerInput", Object.class);
    }
    default Object changedSlots() {
        return require("changedSlots", Object.class);
    }
    default Object carriedItem() {
        return require("carriedItem", Object.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ServerboundContainerClickView withContainerId(int containerId) {
        return (ServerboundContainerClickView) with("containerId", containerId);
    }
    /** Returns a copy with {@code stateId} replaced. */
    default ServerboundContainerClickView withStateId(int stateId) {
        return (ServerboundContainerClickView) with("stateId", stateId);
    }
    /** Returns a copy with {@code slotNum} replaced. */
    default ServerboundContainerClickView withSlotNum(short slotNum) {
        return (ServerboundContainerClickView) with("slotNum", slotNum);
    }
    /** Returns a copy with {@code buttonNum} replaced. */
    default ServerboundContainerClickView withButtonNum(byte buttonNum) {
        return (ServerboundContainerClickView) with("buttonNum", buttonNum);
    }
    /** Returns a copy with {@code containerInput} replaced. */
    default ServerboundContainerClickView withContainerInput(Object containerInput) {
        return (ServerboundContainerClickView) with("containerInput", containerInput);
    }
    /** Returns a copy with {@code changedSlots} replaced. */
    default ServerboundContainerClickView withChangedSlots(Object changedSlots) {
        return (ServerboundContainerClickView) with("changedSlots", changedSlots);
    }
    /** Returns a copy with {@code carriedItem} replaced. */
    default ServerboundContainerClickView withCarriedItem(Object carriedItem) {
        return (ServerboundContainerClickView) with("carriedItem", carriedItem);
    }
}
