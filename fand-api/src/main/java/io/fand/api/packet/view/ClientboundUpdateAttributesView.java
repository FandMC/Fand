package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of an entity attribute update (speed, attack damage, etc.).
 * Read-only. The attribute snapshots are exposed via the dynamic
 * {@code get("attributes", ...)} as an opaque value.
 */
public interface ClientboundUpdateAttributesView extends PacketView {

    default int entityId() {
        return require("entityId", Integer.class);
    }
}
