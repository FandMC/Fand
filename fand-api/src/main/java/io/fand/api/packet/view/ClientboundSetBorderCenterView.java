package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetBorderCenterPacket}. */
public interface ClientboundSetBorderCenterView extends PacketView {

    default double newCenterX() {
        return require("newCenterX", double.class);
    }
    default double newCenterZ() {
        return require("newCenterZ", double.class);
    }

    /** Returns a copy with {@code newCenterX} replaced. */
    default ClientboundSetBorderCenterView withNewCenterX(double newCenterX) {
        return (ClientboundSetBorderCenterView) with("newCenterX", newCenterX);
    }
    /** Returns a copy with {@code newCenterZ} replaced. */
    default ClientboundSetBorderCenterView withNewCenterZ(double newCenterZ) {
        return (ClientboundSetBorderCenterView) with("newCenterZ", newCenterZ);
    }
}
