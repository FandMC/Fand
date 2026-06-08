package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundTeleportToEntityPacket}. */
public interface ServerboundTeleportToEntityView extends PacketView {

    default UUID uuid() {
        return require("uuid", UUID.class);
    }

    /** Returns a copy with {@code uuid} replaced. */
    default ServerboundTeleportToEntityView withUuid(UUID uuid) {
        return (ServerboundTeleportToEntityView) with("uuid", uuid);
    }
}
