package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundContainerSetContentPacket}. */
public interface ClientboundContainerSetContentView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }
    default int stateId() {
        return require("stateId", int.class);
    }
    default Object items() {
        return require("items", Object.class);
    }
    default Object carriedItem() {
        return require("carriedItem", Object.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ClientboundContainerSetContentView withContainerId(int containerId) {
        return (ClientboundContainerSetContentView) with("containerId", containerId);
    }
    /** Returns a copy with {@code stateId} replaced. */
    default ClientboundContainerSetContentView withStateId(int stateId) {
        return (ClientboundContainerSetContentView) with("stateId", stateId);
    }
    /** Returns a copy with {@code items} replaced. */
    default ClientboundContainerSetContentView withItems(Object items) {
        return (ClientboundContainerSetContentView) with("items", items);
    }
    /** Returns a copy with {@code carriedItem} replaced. */
    default ClientboundContainerSetContentView withCarriedItem(Object carriedItem) {
        return (ClientboundContainerSetContentView) with("carriedItem", carriedItem);
    }
}
