package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of a player position sync. The position/rotation change and
 * relative flags are exposed via the dynamic {@code get(...)} as opaque values;
 * the id is replaceable.
 */
public interface ClientboundPlayerPositionView extends PacketView {

    /** The id of the entity being repositioned (the player). */
    default int entityId() {
        return require("id", Integer.class);
    }
}
