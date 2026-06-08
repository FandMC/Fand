package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetSubtitleTextPacket}. */
public interface ClientboundSetSubtitleTextView extends PacketView {

    default Object text() {
        return require("text", Object.class);
    }

    /** Returns a copy with {@code text} replaced. */
    default ClientboundSetSubtitleTextView withText(Object text) {
        return (ClientboundSetSubtitleTextView) with("text", text);
    }
}
