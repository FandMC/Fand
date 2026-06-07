package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of a game-state event (rain start/stop, gamemode change, etc.).
 * Read-only. The event kind is exposed via the dynamic {@code get("event", ...)}
 * as an opaque value.
 */
public interface ClientboundGameEventView extends PacketView {

    /** The event parameter (meaning depends on the event kind). */
    default float param() {
        return require("param", Float.class);
    }
}
