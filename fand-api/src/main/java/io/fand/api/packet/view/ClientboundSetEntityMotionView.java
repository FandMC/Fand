package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import io.fand.api.packet.Vec3d;

/** Typed view of an entity velocity update. Replaceable. */
public interface ClientboundSetEntityMotionView extends PacketView {

    /** The id of the entity whose velocity is being set. */
    default int entityId() {
        return require("id", Integer.class);
    }

    /** The velocity vector, in blocks per tick. */
    default Vec3d velocity() {
        return require("movement", Vec3d.class);
    }

    default double velocityX() {
        return velocity().x();
    }

    default double velocityY() {
        return velocity().y();
    }

    default double velocityZ() {
        return velocity().z();
    }
}
