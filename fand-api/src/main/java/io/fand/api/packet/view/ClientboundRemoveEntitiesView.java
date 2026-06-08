package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundRemoveEntitiesPacket}. */
public interface ClientboundRemoveEntitiesView extends PacketView {

    default Object entityIds() {
        return require("entityIds", Object.class);
    }

    /** Returns a copy with {@code entityIds} replaced. */
    default ClientboundRemoveEntitiesView withEntityIds(Object entityIds) {
        return (ClientboundRemoveEntitiesView) with("entityIds", entityIds);
    }
}
