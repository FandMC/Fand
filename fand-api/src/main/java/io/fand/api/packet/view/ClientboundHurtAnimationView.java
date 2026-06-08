package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundHurtAnimationPacket}. */
public interface ClientboundHurtAnimationView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default float yaw() {
        return require("yaw", float.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundHurtAnimationView withId(int id) {
        return (ClientboundHurtAnimationView) with("id", id);
    }
    /** Returns a copy with {@code yaw} replaced. */
    default ClientboundHurtAnimationView withYaw(float yaw) {
        return (ClientboundHurtAnimationView) with("yaw", yaw);
    }
}
