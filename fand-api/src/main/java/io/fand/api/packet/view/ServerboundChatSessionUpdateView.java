package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundChatSessionUpdatePacket}. */
public interface ServerboundChatSessionUpdateView extends PacketView {

    default Object chatSession() {
        return require("chatSession", Object.class);
    }

    /** Returns a copy with {@code chatSession} replaced. */
    default ServerboundChatSessionUpdateView withChatSession(Object chatSession) {
        return (ServerboundChatSessionUpdateView) with("chatSession", chatSession);
    }
}
