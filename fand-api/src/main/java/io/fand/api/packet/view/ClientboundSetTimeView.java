package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of a world time update. The per-clock updates are exposed via the
 * dynamic {@code get("clockUpdates", ...)} as an opaque value; the game time is
 * replaceable.
 */
public interface ClientboundSetTimeView extends PacketView {

    /** The world game time, in ticks. */
    default long gameTime() {
        return require("gameTime", Long.class);
    }
}
