package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundRegistryDataPacket}. */
public interface ClientboundRegistryDataView extends PacketView {

    default Object registry() {
        return require("registry", Object.class);
    }
    default Object entries() {
        return require("entries", Object.class);
    }

    /** Returns a copy with {@code registry} replaced. */
    default ClientboundRegistryDataView withRegistry(Object registry) {
        return (ClientboundRegistryDataView) with("registry", registry);
    }
    /** Returns a copy with {@code entries} replaced. */
    default ClientboundRegistryDataView withEntries(Object entries) {
        return (ClientboundRegistryDataView) with("entries", entries);
    }
}
