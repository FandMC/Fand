package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetTitleTextPacket}. */
public interface ClientboundSetTitleTextView extends PacketView {

    default Object text() {
        return require("text", Object.class);
    }

    /** Returns a copy with {@code text} replaced. */
    default ClientboundSetTitleTextView withText(Object text) {
        return (ClientboundSetTitleTextView) with("text", text);
    }
}
