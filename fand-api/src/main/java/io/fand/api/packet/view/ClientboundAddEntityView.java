package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import io.fand.api.packet.Vec3d;
import java.util.UUID;

/**
 * Typed view of an entity spawn. Read-only. The entity type is exposed via the
 * dynamic {@code get("type", ...)} as an opaque value; rotation is packed and
 * omitted here.
 */
public interface ClientboundAddEntityView extends PacketView {

    default int entityId() {
        return require("id", Integer.class);
    }

    default UUID uuid() {
        return require("uuid", UUID.class);
    }

    default double x() {
        return require("x", Double.class);
    }

    default double y() {
        return require("y", Double.class);
    }

    default double z() {
        return require("z", Double.class);
    }

    /** Initial velocity. */
    default Vec3d velocity() {
        return require("movement", Vec3d.class);
    }

    /** Type-specific spawn data (e.g. orientation for some entities). */
    default int data() {
        return require("data", Integer.class);
    }
}
