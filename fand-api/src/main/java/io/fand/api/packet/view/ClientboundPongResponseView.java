package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPongResponsePacket}. */
public interface ClientboundPongResponseView extends PacketView {

    default long time() {
        return require("time", long.class);
    }

    /** Returns a copy with {@code time} replaced. */
    default ClientboundPongResponseView withTime(long time) {
        return (ClientboundPongResponseView) with("time", time);
    }
}
