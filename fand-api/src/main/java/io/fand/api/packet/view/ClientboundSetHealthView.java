package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetHealthPacket}. */
public interface ClientboundSetHealthView extends PacketView {

    default float health() {
        return require("health", float.class);
    }
    default int food() {
        return require("food", int.class);
    }
    default float saturation() {
        return require("saturation", float.class);
    }

    /** Returns a copy with {@code health} replaced. */
    default ClientboundSetHealthView withHealth(float health) {
        return (ClientboundSetHealthView) with("health", health);
    }
    /** Returns a copy with {@code food} replaced. */
    default ClientboundSetHealthView withFood(int food) {
        return (ClientboundSetHealthView) with("food", food);
    }
    /** Returns a copy with {@code saturation} replaced. */
    default ClientboundSetHealthView withSaturation(float saturation) {
        return (ClientboundSetHealthView) with("saturation", saturation);
    }
}
