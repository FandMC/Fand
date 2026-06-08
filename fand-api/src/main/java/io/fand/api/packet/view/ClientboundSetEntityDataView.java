package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetEntityDataPacket}. */
public interface ClientboundSetEntityDataView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default Object packedItems() {
        return require("packedItems", Object.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundSetEntityDataView withId(int id) {
        return (ClientboundSetEntityDataView) with("id", id);
    }
    /** Returns a copy with {@code packedItems} replaced. */
    default ClientboundSetEntityDataView withPackedItems(Object packedItems) {
        return (ClientboundSetEntityDataView) with("packedItems", packedItems);
    }
}
