package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundTrackedWaypointPacket}. */
public interface ClientboundTrackedWaypointView extends PacketView {

    default Object operation() {
        return require("operation", Object.class);
    }
    default Object waypoint() {
        return require("waypoint", Object.class);
    }

    /** Returns a copy with {@code operation} replaced. */
    default ClientboundTrackedWaypointView withOperation(Object operation) {
        return (ClientboundTrackedWaypointView) with("operation", operation);
    }
    /** Returns a copy with {@code waypoint} replaced. */
    default ClientboundTrackedWaypointView withWaypoint(Object waypoint) {
        return (ClientboundTrackedWaypointView) with("waypoint", waypoint);
    }
}
