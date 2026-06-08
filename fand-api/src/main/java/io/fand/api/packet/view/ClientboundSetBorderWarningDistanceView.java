package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetBorderWarningDistancePacket}. */
public interface ClientboundSetBorderWarningDistanceView extends PacketView {

    default int warningBlocks() {
        return require("warningBlocks", int.class);
    }

    /** Returns a copy with {@code warningBlocks} replaced. */
    default ClientboundSetBorderWarningDistanceView withWarningBlocks(int warningBlocks) {
        return (ClientboundSetBorderWarningDistanceView) with("warningBlocks", warningBlocks);
    }
}
