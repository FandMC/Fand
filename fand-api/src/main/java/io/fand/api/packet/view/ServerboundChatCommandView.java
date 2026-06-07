package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/** Typed view of a command a player ran (without the leading slash). Replaceable. */
public interface ServerboundChatCommandView extends PacketView {

    /** The command text, without the leading {@code /}. */
    default String command() {
        return require("command", String.class);
    }
}
