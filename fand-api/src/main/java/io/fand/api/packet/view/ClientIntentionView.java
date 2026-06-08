package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientIntentionPacket}. */
public interface ClientIntentionView extends PacketView {

    default int protocolVersion() {
        return require("protocolVersion", int.class);
    }
    default String hostName() {
        return require("hostName", String.class);
    }
    default int port() {
        return require("port", int.class);
    }
    default Object intention() {
        return require("intention", Object.class);
    }

    /** Returns a copy with {@code protocolVersion} replaced. */
    default ClientIntentionView withProtocolVersion(int protocolVersion) {
        return (ClientIntentionView) with("protocolVersion", protocolVersion);
    }
    /** Returns a copy with {@code hostName} replaced. */
    default ClientIntentionView withHostName(String hostName) {
        return (ClientIntentionView) with("hostName", hostName);
    }
    /** Returns a copy with {@code port} replaced. */
    default ClientIntentionView withPort(int port) {
        return (ClientIntentionView) with("port", port);
    }
    /** Returns a copy with {@code intention} replaced. */
    default ClientIntentionView withIntention(Object intention) {
        return (ClientIntentionView) with("intention", intention);
    }
}
