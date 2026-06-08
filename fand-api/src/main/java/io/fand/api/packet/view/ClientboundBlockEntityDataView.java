package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundBlockEntityDataPacket}. */
public interface ClientboundBlockEntityDataView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default Object tag() {
        return require("tag", Object.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ClientboundBlockEntityDataView withPos(Object pos) {
        return (ClientboundBlockEntityDataView) with("pos", pos);
    }
    /** Returns a copy with {@code tag} replaced. */
    default ClientboundBlockEntityDataView withTag(Object tag) {
        return (ClientboundBlockEntityDataView) with("tag", tag);
    }
}
