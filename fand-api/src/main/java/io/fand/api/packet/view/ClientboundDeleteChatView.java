package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundDeleteChatPacket}. */
public interface ClientboundDeleteChatView extends PacketView {

    default Object messageSignature() {
        return require("messageSignature", Object.class);
    }

    /** Returns a copy with {@code messageSignature} replaced. */
    default ClientboundDeleteChatView withMessageSignature(Object messageSignature) {
        return (ClientboundDeleteChatView) with("messageSignature", messageSignature);
    }
}
