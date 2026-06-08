package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundStoreCookiePacket}. */
public interface ClientboundStoreCookieView extends PacketView {

    default Object key() {
        return require("key", Object.class);
    }
    default Object payload() {
        return require("payload", Object.class);
    }

    /** Returns a copy with {@code key} replaced. */
    default ClientboundStoreCookieView withKey(Object key) {
        return (ClientboundStoreCookieView) with("key", key);
    }
    /** Returns a copy with {@code payload} replaced. */
    default ClientboundStoreCookieView withPayload(Object payload) {
        return (ClientboundStoreCookieView) with("payload", payload);
    }
}
