package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetEquipmentPacket}. */
public interface ClientboundSetEquipmentView extends PacketView {

    default int entity() {
        return require("entity", int.class);
    }

    /** Returns a copy with {@code entity} replaced. */
    default ClientboundSetEquipmentView withEntity(int entity) {
        return (ClientboundSetEquipmentView) with("entity", entity);
    }
}
