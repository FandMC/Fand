package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import net.kyori.adventure.text.Component;

/** Typed view of a system chat message (no signature). Replaceable. */
public interface ClientboundSystemChatView extends PacketView {

    /** The message content. */
    default Component content() {
        return require("content", Component.class);
    }

    /** Whether the message renders on the action bar instead of the chat box. */
    default boolean overlay() {
        return require("overlay", Boolean.class);
    }
}
