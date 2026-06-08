package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundTransferPacket}. */
public interface ClientboundTransferView extends PacketView {

    default String host() {
        return require("host", String.class);
    }
    default int port() {
        return require("port", int.class);
    }

    /** Returns a copy with {@code host} replaced. */
    default ClientboundTransferView withHost(String host) {
        return (ClientboundTransferView) with("host", host);
    }
    /** Returns a copy with {@code port} replaced. */
    default ClientboundTransferView withPort(int port) {
        return (ClientboundTransferView) with("port", port);
    }
}
