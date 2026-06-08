package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundServerDataPacket}. */
public interface ClientboundServerDataView extends PacketView {

    default Object motd() {
        return require("motd", Object.class);
    }
    default Object iconBytes() {
        return require("iconBytes", Object.class);
    }

    /** Returns a copy with {@code motd} replaced. */
    default ClientboundServerDataView withMotd(Object motd) {
        return (ClientboundServerDataView) with("motd", motd);
    }
    /** Returns a copy with {@code iconBytes} replaced. */
    default ClientboundServerDataView withIconBytes(Object iconBytes) {
        return (ClientboundServerDataView) with("iconBytes", iconBytes);
    }
}
