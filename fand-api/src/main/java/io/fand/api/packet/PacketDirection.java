package io.fand.api.packet;

/**
 * The travel direction of a packet relative to the server: {@link #INBOUND}
 * from client, {@link #OUTBOUND} to client, or {@link #BIDIRECTIONAL} for
 * custom packet channels that carry payloads in both directions.
 */
public enum PacketDirection {

    /** Client to server (C2S). */
    INBOUND,

    /** Server to client (S2C). */
    OUTBOUND,

    /** Bidirectional (custom packet channels only). */
    BIDIRECTIONAL
}
