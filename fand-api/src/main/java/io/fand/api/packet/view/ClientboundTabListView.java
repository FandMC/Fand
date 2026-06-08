package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundTabListPacket}. */
public interface ClientboundTabListView extends PacketView {

    default Object header() {
        return require("header", Object.class);
    }
    default Object footer() {
        return require("footer", Object.class);
    }

    /** Returns a copy with {@code header} replaced. */
    default ClientboundTabListView withHeader(Object header) {
        return (ClientboundTabListView) with("header", header);
    }
    /** Returns a copy with {@code footer} replaced. */
    default ClientboundTabListView withFooter(Object footer) {
        return (ClientboundTabListView) with("footer", footer);
    }
}
