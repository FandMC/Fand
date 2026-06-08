package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundTeleportEntityPacket}. */
public interface ClientboundTeleportEntityView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default Object change() {
        return require("change", Object.class);
    }
    default Object relatives() {
        return require("relatives", Object.class);
    }
    default boolean onGround() {
        return require("onGround", boolean.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundTeleportEntityView withId(int id) {
        return (ClientboundTeleportEntityView) with("id", id);
    }
    /** Returns a copy with {@code change} replaced. */
    default ClientboundTeleportEntityView withChange(Object change) {
        return (ClientboundTeleportEntityView) with("change", change);
    }
    /** Returns a copy with {@code relatives} replaced. */
    default ClientboundTeleportEntityView withRelatives(Object relatives) {
        return (ClientboundTeleportEntityView) with("relatives", relatives);
    }
    /** Returns a copy with {@code onGround} replaced. */
    default ClientboundTeleportEntityView withOnGround(boolean onGround) {
        return (ClientboundTeleportEntityView) with("onGround", onGround);
    }
}
