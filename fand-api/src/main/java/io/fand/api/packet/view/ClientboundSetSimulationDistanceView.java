package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetSimulationDistancePacket}. */
public interface ClientboundSetSimulationDistanceView extends PacketView {

    default int simulationDistance() {
        return require("simulationDistance", int.class);
    }

    /** Returns a copy with {@code simulationDistance} replaced. */
    default ClientboundSetSimulationDistanceView withSimulationDistance(int simulationDistance) {
        return (ClientboundSetSimulationDistanceView) with("simulationDistance", simulationDistance);
    }
}
