package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundResourcePackPopPacket}. */
public interface ClientboundResourcePackPopView extends PacketView {

    default Object id() {
        return require("id", Object.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundResourcePackPopView withId(Object id) {
        return (ClientboundResourcePackPopView) with("id", id);
    }
}
