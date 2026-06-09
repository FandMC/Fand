package io.fand.api.packet;

import java.util.Locale;

/**
 * Vanilla network protocol phase.
 */
public enum PacketProtocol {
    HANDSHAKING("handshake"),
    PLAY("play"),
    STATUS("status"),
    LOGIN("login"),
    CONFIGURATION("configuration");

    private final String id;

    PacketProtocol(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static PacketProtocol fromId(String id) {
        for (var protocol : values()) {
            if (protocol.id.equals(id)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("Unknown packet protocol: " + id);
    }

    public static PacketProtocol fromName(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
