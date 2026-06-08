package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundCustomChatCompletionsPacket}. */
public interface ClientboundCustomChatCompletionsView extends PacketView {

    default Object action() {
        return require("action", Object.class);
    }
    default Object entries() {
        return require("entries", Object.class);
    }

    /** Returns a copy with {@code action} replaced. */
    default ClientboundCustomChatCompletionsView withAction(Object action) {
        return (ClientboundCustomChatCompletionsView) with("action", action);
    }
    /** Returns a copy with {@code entries} replaced. */
    default ClientboundCustomChatCompletionsView withEntries(Object entries) {
        return (ClientboundCustomChatCompletionsView) with("entries", entries);
    }
}
