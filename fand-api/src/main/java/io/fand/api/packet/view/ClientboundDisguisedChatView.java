package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundDisguisedChatPacket}. */
public interface ClientboundDisguisedChatView extends PacketView {

    default Object message() {
        return require("message", Object.class);
    }
    default Object chatType() {
        return require("chatType", Object.class);
    }

    /** Returns a copy with {@code message} replaced. */
    default ClientboundDisguisedChatView withMessage(Object message) {
        return (ClientboundDisguisedChatView) with("message", message);
    }
    /** Returns a copy with {@code chatType} replaced. */
    default ClientboundDisguisedChatView withChatType(Object chatType) {
        return (ClientboundDisguisedChatView) with("chatType", chatType);
    }
}
