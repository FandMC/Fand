package io.fand.api.packet;

import java.util.Objects;

/**
 * Describes a custom packet channel: the wire identity, the direction it may
 * travel, and the payload record carried over it.
 *
 * <p>The {@code key} is a namespaced string {@code namespace:path}. The
 * {@code namespace} must not be {@code bungeecord} or {@code velocity}; those
 * are reserved for proxy plugin messaging and are rejected at registration time.
 *
 * @param <P>     the payload record type
 * @param key     the channel identifier in {@code namespace:path} form
 * @param direction the direction this packet is allowed to travel
 * @param payloadType the payload record class
 */
public record CustomPacketDefinition<P extends Record>(
        String key,
        PacketDirection direction,
        Class<P> payloadType
) {

    public CustomPacketDefinition {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(payloadType, "payloadType");
        if (key.indexOf(':') < 0) {
            throw new IllegalArgumentException("key must be in namespace:path form, got: " + key);
        }
    }

    /** The namespace portion of {@link #key} (before the first colon). */
    public String namespace() {
        return key.substring(0, key.indexOf(':'));
    }

    /** The path portion of {@link #key} (after the first colon). */
    public String path() {
        return key.substring(key.indexOf(':') + 1);
    }
}
