package io.fand.api.packet.view;

import io.fand.api.packet.BlockPosition;
import io.fand.api.packet.PacketView;

/**
 * Typed view of a single block change. Read-only. The block state is exposed
 * via the dynamic {@code get("blockState", ...)} as an opaque value.
 */
public interface ClientboundBlockUpdateView extends PacketView {

    default BlockPosition position() {
        return require("pos", BlockPosition.class);
    }
}
