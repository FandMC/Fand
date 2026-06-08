package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetActionBarTextPacket}. */
public interface ClientboundSetActionBarTextView extends PacketView {

    default Object text() {
        return require("text", Object.class);
    }

    /** Returns a copy with {@code text} replaced. */
    default ClientboundSetActionBarTextView withText(Object text) {
        return (ClientboundSetActionBarTextView) with("text", text);
    }
}
