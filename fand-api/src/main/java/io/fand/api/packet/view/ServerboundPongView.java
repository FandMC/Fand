package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundPongPacket}. */
public interface ServerboundPongView extends PacketView {

    default int id() {
        return require("id", int.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ServerboundPongView withId(int id) {
        return (ServerboundPongView) with("id", id);
    }
}
