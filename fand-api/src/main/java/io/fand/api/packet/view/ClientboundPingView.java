package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPingPacket}. */
public interface ClientboundPingView extends PacketView {

    default int id() {
        return require("id", int.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundPingView withId(int id) {
        return (ClientboundPingView) with("id", id);
    }
}
