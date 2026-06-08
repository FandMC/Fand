package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundResourcePackPacket}. */
public interface ServerboundResourcePackView extends PacketView {

    default UUID id() {
        return require("id", UUID.class);
    }
    default Object action() {
        return require("action", Object.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ServerboundResourcePackView withId(UUID id) {
        return (ServerboundResourcePackView) with("id", id);
    }
    /** Returns a copy with {@code action} replaced. */
    default ServerboundResourcePackView withAction(Object action) {
        return (ServerboundResourcePackView) with("action", action);
    }
}
