package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetBorderWarningDelayPacket}. */
public interface ClientboundSetBorderWarningDelayView extends PacketView {

    default int warningDelay() {
        return require("warningDelay", int.class);
    }

    /** Returns a copy with {@code warningDelay} replaced. */
    default ClientboundSetBorderWarningDelayView withWarningDelay(int warningDelay) {
        return (ClientboundSetBorderWarningDelayView) with("warningDelay", warningDelay);
    }
}
