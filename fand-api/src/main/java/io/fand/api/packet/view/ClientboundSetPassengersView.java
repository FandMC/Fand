package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetPassengersPacket}. */
public interface ClientboundSetPassengersView extends PacketView {

    default int vehicle() {
        return require("vehicle", int.class);
    }
    default Object passengers() {
        return require("passengers", Object.class);
    }

    /** Returns a copy with {@code vehicle} replaced. */
    default ClientboundSetPassengersView withVehicle(int vehicle) {
        return (ClientboundSetPassengersView) with("vehicle", vehicle);
    }
    /** Returns a copy with {@code passengers} replaced. */
    default ClientboundSetPassengersView withPassengers(Object passengers) {
        return (ClientboundSetPassengersView) with("passengers", passengers);
    }
}
