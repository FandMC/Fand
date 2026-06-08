package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundContainerClosePacket}. */
public interface ClientboundContainerCloseView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ClientboundContainerCloseView withContainerId(int containerId) {
        return (ClientboundContainerCloseView) with("containerId", containerId);
    }
}
