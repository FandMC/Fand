package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundMountScreenOpenPacket}. */
public interface ClientboundMountScreenOpenView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }
    default int inventoryColumns() {
        return require("inventoryColumns", int.class);
    }
    default int entityId() {
        return require("entityId", int.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ClientboundMountScreenOpenView withContainerId(int containerId) {
        return (ClientboundMountScreenOpenView) with("containerId", containerId);
    }
    /** Returns a copy with {@code inventoryColumns} replaced. */
    default ClientboundMountScreenOpenView withInventoryColumns(int inventoryColumns) {
        return (ClientboundMountScreenOpenView) with("inventoryColumns", inventoryColumns);
    }
    /** Returns a copy with {@code entityId} replaced. */
    default ClientboundMountScreenOpenView withEntityId(int entityId) {
        return (ClientboundMountScreenOpenView) with("entityId", entityId);
    }
}
