package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/** Typed view of a chat message sent by a player. Replaceable. */
public interface ServerboundChatView extends PacketView {

    /** The raw chat message text. */
    default String message() {
        return require("message", String.class);
    }

    /** The client-supplied salt used for message signing. */
    default long salt() {
        return require("salt", Long.class);
    }
}
