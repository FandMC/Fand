package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/**
 * Typed view of a signed player chat message. The signature, message body,
 * filter mask, and chat type are exposed via the dynamic {@code get(...)} as
 * opaque values — rewriting signed content safely requires re-signing, so this
 * view focuses on routing fields.
 */
public interface ClientboundPlayerChatView extends PacketView {

    /** The sending player's UUID. */
    default UUID sender() {
        return require("sender", UUID.class);
    }

    /** The per-sender message index. */
    default int index() {
        return require("index", Integer.class);
    }

    /** The server-global message index. */
    default int globalIndex() {
        return require("globalIndex", Integer.class);
    }
}
