package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundCookieRequestPacket}. */
public interface ClientboundCookieRequestView extends PacketView {

    default Object key() {
        return require("key", Object.class);
    }

    /** Returns a copy with {@code key} replaced. */
    default ClientboundCookieRequestView withKey(Object key) {
        return (ClientboundCookieRequestView) with("key", key);
    }
}
