package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundLightUpdatePacket}. */
public interface ClientboundLightUpdateView extends PacketView {

    default int x() {
        return require("x", int.class);
    }
    default int z() {
        return require("z", int.class);
    }
    default Object lightData() {
        return require("lightData", Object.class);
    }

    /** Returns a copy with {@code x} replaced. */
    default ClientboundLightUpdateView withX(int x) {
        return (ClientboundLightUpdateView) with("x", x);
    }
    /** Returns a copy with {@code z} replaced. */
    default ClientboundLightUpdateView withZ(int z) {
        return (ClientboundLightUpdateView) with("z", z);
    }
    /** Returns a copy with {@code lightData} replaced. */
    default ClientboundLightUpdateView withLightData(Object lightData) {
        return (ClientboundLightUpdateView) with("lightData", lightData);
    }
}
