package io.fand.api.packet;

/**
 * Packet interceptor invocation order. Lower-priority interceptors run first,
 * allowing higher-priority interceptors to enforce final packet invariants.
 */
public enum PacketPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST
}
