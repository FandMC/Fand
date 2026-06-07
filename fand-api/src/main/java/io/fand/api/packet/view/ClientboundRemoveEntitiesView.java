package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/** Typed view of an entity-removal batch. Read-only. */
public interface ClientboundRemoveEntitiesView extends PacketView {

    /** The ids of the entities being removed. */
    default int[] entityIds() {
        return require("entityIds", int[].class);
    }
}
