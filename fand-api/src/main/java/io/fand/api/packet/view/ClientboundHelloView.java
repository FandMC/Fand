package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundHelloPacket}. */
public interface ClientboundHelloView extends PacketView {

    default String serverId() {
        return require("serverId", String.class);
    }
    default Object publicKey() {
        return require("publicKey", Object.class);
    }
    default Object challenge() {
        return require("challenge", Object.class);
    }
    default boolean shouldAuthenticate() {
        return require("shouldAuthenticate", boolean.class);
    }

    /** Returns a copy with {@code serverId} replaced. */
    default ClientboundHelloView withServerId(String serverId) {
        return (ClientboundHelloView) with("serverId", serverId);
    }
    /** Returns a copy with {@code publicKey} replaced. */
    default ClientboundHelloView withPublicKey(Object publicKey) {
        return (ClientboundHelloView) with("publicKey", publicKey);
    }
    /** Returns a copy with {@code challenge} replaced. */
    default ClientboundHelloView withChallenge(Object challenge) {
        return (ClientboundHelloView) with("challenge", challenge);
    }
    /** Returns a copy with {@code shouldAuthenticate} replaced. */
    default ClientboundHelloView withShouldAuthenticate(boolean shouldAuthenticate) {
        return (ClientboundHelloView) with("shouldAuthenticate", shouldAuthenticate);
    }
}
