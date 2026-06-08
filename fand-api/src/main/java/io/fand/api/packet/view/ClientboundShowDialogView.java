package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundShowDialogPacket}. */
public interface ClientboundShowDialogView extends PacketView {

    default Object dialog() {
        return require("dialog", Object.class);
    }

    /** Returns a copy with {@code dialog} replaced. */
    default ClientboundShowDialogView withDialog(Object dialog) {
        return (ClientboundShowDialogView) with("dialog", dialog);
    }
}
