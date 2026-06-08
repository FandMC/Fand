package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundKeepAlivePacket}. */
public interface ServerboundKeepAliveView extends PacketView {

    default long id() {
        return require("id", long.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ServerboundKeepAliveView withId(long id) {
        return (ServerboundKeepAliveView) with("id", id);
    }
}
