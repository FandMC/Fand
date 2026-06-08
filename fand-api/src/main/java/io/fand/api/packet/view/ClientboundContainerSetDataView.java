package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundContainerSetDataPacket}. */
public interface ClientboundContainerSetDataView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }
    default int id() {
        return require("id", int.class);
    }
    default int value() {
        return require("value", int.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ClientboundContainerSetDataView withContainerId(int containerId) {
        return (ClientboundContainerSetDataView) with("containerId", containerId);
    }
    /** Returns a copy with {@code id} replaced. */
    default ClientboundContainerSetDataView withId(int id) {
        return (ClientboundContainerSetDataView) with("id", id);
    }
    /** Returns a copy with {@code value} replaced. */
    default ClientboundContainerSetDataView withValue(int value) {
        return (ClientboundContainerSetDataView) with("value", value);
    }
}
