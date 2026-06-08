package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetDefaultSpawnPositionPacket}. */
public interface ClientboundSetDefaultSpawnPositionView extends PacketView {

    default Object respawnData() {
        return require("respawnData", Object.class);
    }

    /** Returns a copy with {@code respawnData} replaced. */
    default ClientboundSetDefaultSpawnPositionView withRespawnData(Object respawnData) {
        return (ClientboundSetDefaultSpawnPositionView) with("respawnData", respawnData);
    }
}
