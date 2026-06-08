package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundClientCommandPacket}. */
public interface ServerboundClientCommandView extends PacketView {

    default Object action() {
        return require("action", Object.class);
    }

    /** Returns a copy with {@code action} replaced. */
    default ServerboundClientCommandView withAction(Object action) {
        return (ServerboundClientCommandView) with("action", action);
    }
}
