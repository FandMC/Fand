package io.fand.api.packet.view;

import io.fand.api.packet.BlockPosition;
import io.fand.api.packet.PacketView;

/**
 * Typed view of a block-entity data update. Read-only. The block-entity type
 * and NBT tag are exposed via the dynamic {@code get(...)} as opaque values.
 */
public interface ClientboundBlockEntityDataView extends PacketView {

    default BlockPosition position() {
        return require("pos", BlockPosition.class);
    }
}
