package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundEntityPositionSyncPacket}. */
public interface ClientboundEntityPositionSyncView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default Object values() {
        return require("values", Object.class);
    }
    default boolean onGround() {
        return require("onGround", boolean.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundEntityPositionSyncView withId(int id) {
        return (ClientboundEntityPositionSyncView) with("id", id);
    }
    /** Returns a copy with {@code values} replaced. */
    default ClientboundEntityPositionSyncView withValues(Object values) {
        return (ClientboundEntityPositionSyncView) with("values", values);
    }
    /** Returns a copy with {@code onGround} replaced. */
    default ClientboundEntityPositionSyncView withOnGround(boolean onGround) {
        return (ClientboundEntityPositionSyncView) with("onGround", onGround);
    }
}
