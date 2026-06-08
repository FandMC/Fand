package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetBorderLerpSizePacket}. */
public interface ClientboundSetBorderLerpSizeView extends PacketView {

    default double oldSize() {
        return require("oldSize", double.class);
    }
    default double newSize() {
        return require("newSize", double.class);
    }
    default long lerpTime() {
        return require("lerpTime", long.class);
    }

    /** Returns a copy with {@code oldSize} replaced. */
    default ClientboundSetBorderLerpSizeView withOldSize(double oldSize) {
        return (ClientboundSetBorderLerpSizeView) with("oldSize", oldSize);
    }
    /** Returns a copy with {@code newSize} replaced. */
    default ClientboundSetBorderLerpSizeView withNewSize(double newSize) {
        return (ClientboundSetBorderLerpSizeView) with("newSize", newSize);
    }
    /** Returns a copy with {@code lerpTime} replaced. */
    default ClientboundSetBorderLerpSizeView withLerpTime(long lerpTime) {
        return (ClientboundSetBorderLerpSizeView) with("lerpTime", lerpTime);
    }
}
