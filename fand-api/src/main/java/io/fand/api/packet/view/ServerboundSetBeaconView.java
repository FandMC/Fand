package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSetBeaconPacket}. */
public interface ServerboundSetBeaconView extends PacketView {

    default Object primary() {
        return require("primary", Object.class);
    }
    default Object secondary() {
        return require("secondary", Object.class);
    }

    /** Returns a copy with {@code primary} replaced. */
    default ServerboundSetBeaconView withPrimary(Object primary) {
        return (ServerboundSetBeaconView) with("primary", primary);
    }
    /** Returns a copy with {@code secondary} replaced. */
    default ServerboundSetBeaconView withSecondary(Object secondary) {
        return (ServerboundSetBeaconView) with("secondary", secondary);
    }
}
