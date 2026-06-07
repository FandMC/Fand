package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of an entity teleport. The position/rotation change and relative
 * flags are exposed via the dynamic {@code get(...)} as opaque values; the
 * scalar fields below are replaceable.
 */
public interface ClientboundTeleportEntityView extends PacketView {

    /** The id of the teleported entity. */
    default int entityId() {
        return require("id", Integer.class);
    }

    default boolean onGround() {
        return require("onGround", Boolean.class);
    }
}
