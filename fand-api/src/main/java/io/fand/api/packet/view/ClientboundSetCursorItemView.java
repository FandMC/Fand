package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetCursorItemPacket}. */
public interface ClientboundSetCursorItemView extends PacketView {

    default Object contents() {
        return require("contents", Object.class);
    }

    /** Returns a copy with {@code contents} replaced. */
    default ClientboundSetCursorItemView withContents(Object contents) {
        return (ClientboundSetCursorItemView) with("contents", contents);
    }
}
