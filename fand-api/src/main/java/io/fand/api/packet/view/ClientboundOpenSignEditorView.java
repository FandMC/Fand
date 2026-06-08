package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundOpenSignEditorPacket}. */
public interface ClientboundOpenSignEditorView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default boolean isFrontText() {
        return require("isFrontText", boolean.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ClientboundOpenSignEditorView withPos(Object pos) {
        return (ClientboundOpenSignEditorView) with("pos", pos);
    }
    /** Returns a copy with {@code isFrontText} replaced. */
    default ClientboundOpenSignEditorView withIsFrontText(boolean isFrontText) {
        return (ClientboundOpenSignEditorView) with("isFrontText", isFrontText);
    }
}
