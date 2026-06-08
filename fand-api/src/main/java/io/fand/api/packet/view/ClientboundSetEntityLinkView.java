package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetEntityLinkPacket}. */
public interface ClientboundSetEntityLinkView extends PacketView {

    default int sourceId() {
        return require("sourceId", int.class);
    }
    default int destId() {
        return require("destId", int.class);
    }

    /** Returns a copy with {@code sourceId} replaced. */
    default ClientboundSetEntityLinkView withSourceId(int sourceId) {
        return (ClientboundSetEntityLinkView) with("sourceId", sourceId);
    }
    /** Returns a copy with {@code destId} replaced. */
    default ClientboundSetEntityLinkView withDestId(int destId) {
        return (ClientboundSetEntityLinkView) with("destId", destId);
    }
}
