package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundOpenScreenPacket}. */
public interface ClientboundOpenScreenView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }
    default Object title() {
        return require("title", Object.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ClientboundOpenScreenView withContainerId(int containerId) {
        return (ClientboundOpenScreenView) with("containerId", containerId);
    }
    /** Returns a copy with {@code title} replaced. */
    default ClientboundOpenScreenView withTitle(Object title) {
        return (ClientboundOpenScreenView) with("title", title);
    }
}
