package io.fand.api.packet;

/**
 * Packet flow from the vanilla protocol perspective.
 */
public enum PacketDirection {
    /** Sent by the server to the client. */
    CLIENTBOUND,

    /** Sent by the client to the server. */
    SERVERBOUND,

    /** Custom channel is valid in both directions. */
    BIDIRECTIONAL;

    public boolean allowsClientbound() {
        return this == CLIENTBOUND || this == BIDIRECTIONAL;
    }

    public boolean allowsServerbound() {
        return this == SERVERBOUND || this == BIDIRECTIONAL;
    }
}
