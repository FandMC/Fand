package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundCustomClickActionPacket}. */
public interface ServerboundCustomClickActionView extends PacketView {

    default Object id() {
        return require("id", Object.class);
    }
    default Object payload() {
        return require("payload", Object.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ServerboundCustomClickActionView withId(Object id) {
        return (ServerboundCustomClickActionView) with("id", id);
    }
    /** Returns a copy with {@code payload} replaced. */
    default ServerboundCustomClickActionView withPayload(Object payload) {
        return (ServerboundCustomClickActionView) with("payload", payload);
    }
}
