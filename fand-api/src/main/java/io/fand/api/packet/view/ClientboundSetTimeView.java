package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetTimePacket}. */
public interface ClientboundSetTimeView extends PacketView {

    default long gameTime() {
        return require("gameTime", long.class);
    }
    default Object clockUpdates() {
        return require("clockUpdates", Object.class);
    }

    /** Returns a copy with {@code gameTime} replaced. */
    default ClientboundSetTimeView withGameTime(long gameTime) {
        return (ClientboundSetTimeView) with("gameTime", gameTime);
    }
    /** Returns a copy with {@code clockUpdates} replaced. */
    default ClientboundSetTimeView withClockUpdates(Object clockUpdates) {
        return (ClientboundSetTimeView) with("clockUpdates", clockUpdates);
    }
}
