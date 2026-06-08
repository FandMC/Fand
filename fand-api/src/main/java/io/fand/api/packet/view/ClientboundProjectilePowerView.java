package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundProjectilePowerPacket}. */
public interface ClientboundProjectilePowerView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default double accelerationPower() {
        return require("accelerationPower", double.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundProjectilePowerView withId(int id) {
        return (ClientboundProjectilePowerView) with("id", id);
    }
    /** Returns a copy with {@code accelerationPower} replaced. */
    default ClientboundProjectilePowerView withAccelerationPower(double accelerationPower) {
        return (ClientboundProjectilePowerView) with("accelerationPower", accelerationPower);
    }
}
