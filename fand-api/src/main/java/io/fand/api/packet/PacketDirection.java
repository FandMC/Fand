package io.fand.api.packet;

/**
 * The direction a packet travels relative to the server.
 */
public enum PacketDirection {

    /** Client to server. */
    INBOUND,

    /** Server to client. */
    OUTBOUND
}
