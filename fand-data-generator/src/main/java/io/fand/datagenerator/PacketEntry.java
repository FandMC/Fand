package io.fand.datagenerator;

import java.util.Objects;

record PacketEntry(
        String enumName,
        String protocolName,
        String directionName,
        String key,
        String sourceClassName,
        PacketViewModel view
) {

    PacketEntry {
        Objects.requireNonNull(enumName, "enumName");
        Objects.requireNonNull(protocolName, "protocolName");
        Objects.requireNonNull(directionName, "directionName");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(sourceClassName, "sourceClassName");
        Objects.requireNonNull(view, "view");
    }
}
