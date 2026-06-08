package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundAcceptTeleportationPacket}. */
public interface ServerboundAcceptTeleportationView extends PacketView {

    default int id() {
        return require("id", int.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ServerboundAcceptTeleportationView withId(int id) {
        return (ServerboundAcceptTeleportationView) with("id", id);
    }
}
