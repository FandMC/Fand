package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetEntityMotionPacket}. */
public interface ClientboundSetEntityMotionView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default Object movement() {
        return require("movement", Object.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundSetEntityMotionView withId(int id) {
        return (ClientboundSetEntityMotionView) with("id", id);
    }
    /** Returns a copy with {@code movement} replaced. */
    default ClientboundSetEntityMotionView withMovement(Object movement) {
        return (ClientboundSetEntityMotionView) with("movement", movement);
    }
}
