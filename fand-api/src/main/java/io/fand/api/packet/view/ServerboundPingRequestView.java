package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundPingRequestPacket}. */
public interface ServerboundPingRequestView extends PacketView {

    default long time() {
        return require("time", long.class);
    }

    /** Returns a copy with {@code time} replaced. */
    default ServerboundPingRequestView withTime(long time) {
        return (ServerboundPingRequestView) with("time", time);
    }
}
