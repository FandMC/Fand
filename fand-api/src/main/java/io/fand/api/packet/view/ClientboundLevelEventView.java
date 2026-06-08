package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundLevelEventPacket}. */
public interface ClientboundLevelEventView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default int data() {
        return require("data", int.class);
    }
    default boolean globalEvent() {
        return require("globalEvent", boolean.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ClientboundLevelEventView withPos(Object pos) {
        return (ClientboundLevelEventView) with("pos", pos);
    }
    /** Returns a copy with {@code data} replaced. */
    default ClientboundLevelEventView withData(int data) {
        return (ClientboundLevelEventView) with("data", data);
    }
    /** Returns a copy with {@code globalEvent} replaced. */
    default ClientboundLevelEventView withGlobalEvent(boolean globalEvent) {
        return (ClientboundLevelEventView) with("globalEvent", globalEvent);
    }
}
