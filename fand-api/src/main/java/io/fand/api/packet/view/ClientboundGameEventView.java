package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundGameEventPacket}. */
public interface ClientboundGameEventView extends PacketView {

    default Object event() {
        return require("event", Object.class);
    }
    default float param() {
        return require("param", float.class);
    }
    default int id() {
        return require("id", int.class);
    }

    /** Returns a copy with {@code event} replaced. */
    default ClientboundGameEventView withEvent(Object event) {
        return (ClientboundGameEventView) with("event", event);
    }
    /** Returns a copy with {@code param} replaced. */
    default ClientboundGameEventView withParam(float param) {
        return (ClientboundGameEventView) with("param", param);
    }
    /** Returns a copy with {@code id} replaced. */
    default ClientboundGameEventView withId(int id) {
        return (ClientboundGameEventView) with("id", id);
    }
}
