package io.fand.api.packet;

import java.util.Set;

/**
 * Public, version-generated view of a vanilla packet.
 */
public interface PacketView {

    PacketType packetType();

    Set<String> fields();

    boolean has(String field);

    Object value(String field);

    <T> T value(String field, Class<T> type);

    PacketView with(String field, Object value);

    <T extends PacketView> T as(Class<T> viewType);

    default <T extends PacketView> T with(String field, Object value, Class<T> viewType) {
        return with(field, value).as(viewType);
    }
}
