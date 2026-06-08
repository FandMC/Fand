package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSeenAdvancementsPacket}. */
public interface ServerboundSeenAdvancementsView extends PacketView {

    default Object action() {
        return require("action", Object.class);
    }

    /** Returns a copy with {@code action} replaced. */
    default ServerboundSeenAdvancementsView withAction(Object action) {
        return (ServerboundSeenAdvancementsView) with("action", action);
    }
}
