package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetCameraPacket}. */
public interface ClientboundSetCameraView extends PacketView {

    default int cameraId() {
        return require("cameraId", int.class);
    }

    /** Returns a copy with {@code cameraId} replaced. */
    default ClientboundSetCameraView withCameraId(int cameraId) {
        return (ClientboundSetCameraView) with("cameraId", cameraId);
    }
}
