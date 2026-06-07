package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of an entity metadata update. The packed metadata entries are
 * exposed via the dynamic {@code get("packedItems", ...)} as an opaque value;
 * the entity id is replaceable.
 */
public interface ClientboundSetEntityDataView extends PacketView {

    default int entityId() {
        return require("id", Integer.class);
    }
}
