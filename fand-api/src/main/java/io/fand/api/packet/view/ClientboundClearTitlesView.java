package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundClearTitlesPacket}. */
public interface ClientboundClearTitlesView extends PacketView {

    default boolean resetTimes() {
        return require("resetTimes", boolean.class);
    }

    /** Returns a copy with {@code resetTimes} replaced. */
    default ClientboundClearTitlesView withResetTimes(boolean resetTimes) {
        return (ClientboundClearTitlesView) with("resetTimes", resetTimes);
    }
}
