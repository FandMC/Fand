package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundKeepAlivePacket}. */
public interface ClientboundKeepAliveView extends PacketView {

    default long id() {
        return require("id", long.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundKeepAliveView withId(long id) {
        return (ClientboundKeepAliveView) with("id", id);
    }
}
