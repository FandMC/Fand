package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of a respawn / dimension change. The spawn info is exposed via the
 * dynamic {@code get("commonPlayerSpawnInfo", ...)} as an opaque value; the
 * data-keep mask is replaceable.
 */
public interface ClientboundRespawnView extends PacketView {

    /** Bitmask of which client data to keep across the respawn. */
    default byte dataToKeep() {
        return require("dataToKeep", Byte.class);
    }
}
