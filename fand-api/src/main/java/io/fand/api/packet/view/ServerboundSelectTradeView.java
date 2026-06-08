package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSelectTradePacket}. */
public interface ServerboundSelectTradeView extends PacketView {

    default int item() {
        return require("item", int.class);
    }

    /** Returns a copy with {@code item} replaced. */
    default ServerboundSelectTradeView withItem(int item) {
        return (ServerboundSelectTradeView) with("item", item);
    }
}
