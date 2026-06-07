package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of an entity equipment update. Read-only. The per-slot item list
 * is exposed via the dynamic {@code get("slots", ...)} as an opaque value.
 */
public interface ClientboundSetEquipmentView extends PacketView {

    default int entityId() {
        return require("entity", Integer.class);
    }
}
