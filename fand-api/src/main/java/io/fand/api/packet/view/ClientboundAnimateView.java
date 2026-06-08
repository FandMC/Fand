package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundAnimatePacket}. */
public interface ClientboundAnimateView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default int action() {
        return require("action", int.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundAnimateView withId(int id) {
        return (ClientboundAnimateView) with("id", id);
    }
    /** Returns a copy with {@code action} replaced. */
    default ClientboundAnimateView withAction(int action) {
        return (ClientboundAnimateView) with("action", action);
    }
}
