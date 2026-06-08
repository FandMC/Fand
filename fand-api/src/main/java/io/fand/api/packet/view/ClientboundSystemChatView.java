package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSystemChatPacket}. */
public interface ClientboundSystemChatView extends PacketView {

    default Object content() {
        return require("content", Object.class);
    }
    default boolean overlay() {
        return require("overlay", boolean.class);
    }

    /** Returns a copy with {@code content} replaced. */
    default ClientboundSystemChatView withContent(Object content) {
        return (ClientboundSystemChatView) with("content", content);
    }
    /** Returns a copy with {@code overlay} replaced. */
    default ClientboundSystemChatView withOverlay(boolean overlay) {
        return (ClientboundSystemChatView) with("overlay", overlay);
    }
}
