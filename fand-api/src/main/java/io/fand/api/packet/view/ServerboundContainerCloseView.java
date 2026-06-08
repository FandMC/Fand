package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundContainerClosePacket}. */
public interface ServerboundContainerCloseView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ServerboundContainerCloseView withContainerId(int containerId) {
        return (ServerboundContainerCloseView) with("containerId", containerId);
    }
}
