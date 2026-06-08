package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSetGameRulePacket}. */
public interface ServerboundSetGameRuleView extends PacketView {

    default Object entries() {
        return require("entries", Object.class);
    }

    /** Returns a copy with {@code entries} replaced. */
    default ServerboundSetGameRuleView withEntries(Object entries) {
        return (ServerboundSetGameRuleView) with("entries", entries);
    }
}
