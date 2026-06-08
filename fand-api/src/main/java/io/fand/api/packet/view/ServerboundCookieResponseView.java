package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundCookieResponsePacket}. */
public interface ServerboundCookieResponseView extends PacketView {

    default Object key() {
        return require("key", Object.class);
    }
    default Object payload() {
        return require("payload", Object.class);
    }

    /** Returns a copy with {@code key} replaced. */
    default ServerboundCookieResponseView withKey(Object key) {
        return (ServerboundCookieResponseView) with("key", key);
    }
    /** Returns a copy with {@code payload} replaced. */
    default ServerboundCookieResponseView withPayload(Object payload) {
        return (ServerboundCookieResponseView) with("payload", payload);
    }
}
