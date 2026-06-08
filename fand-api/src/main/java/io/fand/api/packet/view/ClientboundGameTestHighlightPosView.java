package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundGameTestHighlightPosPacket}. */
public interface ClientboundGameTestHighlightPosView extends PacketView {

    default Object absolutePos() {
        return require("absolutePos", Object.class);
    }
    default Object relativePos() {
        return require("relativePos", Object.class);
    }

    /** Returns a copy with {@code absolutePos} replaced. */
    default ClientboundGameTestHighlightPosView withAbsolutePos(Object absolutePos) {
        return (ClientboundGameTestHighlightPosView) with("absolutePos", absolutePos);
    }
    /** Returns a copy with {@code relativePos} replaced. */
    default ClientboundGameTestHighlightPosView withRelativePos(Object relativePos) {
        return (ClientboundGameTestHighlightPosView) with("relativePos", relativePos);
    }
}
