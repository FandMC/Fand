package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundChatCommandPacket}. */
public interface ServerboundChatCommandView extends PacketView {

    default String command() {
        return require("command", String.class);
    }

    /** Returns a copy with {@code command} replaced. */
    default ServerboundChatCommandView withCommand(String command) {
        return (ServerboundChatCommandView) with("command", command);
    }
}
