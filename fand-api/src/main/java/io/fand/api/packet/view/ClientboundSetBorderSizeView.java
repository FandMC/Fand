package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetBorderSizePacket}. */
public interface ClientboundSetBorderSizeView extends PacketView {

    default double size() {
        return require("size", double.class);
    }

    /** Returns a copy with {@code size} replaced. */
    default ClientboundSetBorderSizeView withSize(double size) {
        return (ClientboundSetBorderSizeView) with("size", size);
    }
}
