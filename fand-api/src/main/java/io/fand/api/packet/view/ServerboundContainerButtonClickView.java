package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundContainerButtonClickPacket}. */
public interface ServerboundContainerButtonClickView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }
    default int buttonId() {
        return require("buttonId", int.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ServerboundContainerButtonClickView withContainerId(int containerId) {
        return (ServerboundContainerButtonClickView) with("containerId", containerId);
    }
    /** Returns a copy with {@code buttonId} replaced. */
    default ServerboundContainerButtonClickView withButtonId(int buttonId) {
        return (ServerboundContainerButtonClickView) with("buttonId", buttonId);
    }
}
