package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPlayerPositionPacket}. */
public interface ClientboundPlayerPositionView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default Object change() {
        return require("change", Object.class);
    }
    default Object relatives() {
        return require("relatives", Object.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundPlayerPositionView withId(int id) {
        return (ClientboundPlayerPositionView) with("id", id);
    }
    /** Returns a copy with {@code change} replaced. */
    default ClientboundPlayerPositionView withChange(Object change) {
        return (ClientboundPlayerPositionView) with("change", change);
    }
    /** Returns a copy with {@code relatives} replaced. */
    default ClientboundPlayerPositionView withRelatives(Object relatives) {
        return (ClientboundPlayerPositionView) with("relatives", relatives);
    }
}
