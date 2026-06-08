package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundServerLinksPacket}. */
public interface ClientboundServerLinksView extends PacketView {

    default Object links() {
        return require("links", Object.class);
    }

    /** Returns a copy with {@code links} replaced. */
    default ClientboundServerLinksView withLinks(Object links) {
        return (ClientboundServerLinksView) with("links", links);
    }
}
