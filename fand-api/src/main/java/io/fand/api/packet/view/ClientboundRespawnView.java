package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundRespawnPacket}. */
public interface ClientboundRespawnView extends PacketView {

    default Object commonPlayerSpawnInfo() {
        return require("commonPlayerSpawnInfo", Object.class);
    }
    default byte dataToKeep() {
        return require("dataToKeep", byte.class);
    }

    /** Returns a copy with {@code commonPlayerSpawnInfo} replaced. */
    default ClientboundRespawnView withCommonPlayerSpawnInfo(Object commonPlayerSpawnInfo) {
        return (ClientboundRespawnView) with("commonPlayerSpawnInfo", commonPlayerSpawnInfo);
    }
    /** Returns a copy with {@code dataToKeep} replaced. */
    default ClientboundRespawnView withDataToKeep(byte dataToKeep) {
        return (ClientboundRespawnView) with("dataToKeep", dataToKeep);
    }
}
