package io.fand.api.messaging;

public enum PluginMessageDirection {
    CLIENTBOUND,
    SERVERBOUND,
    BIDIRECTIONAL;

    public boolean allowsClientbound() {
        return this == CLIENTBOUND || this == BIDIRECTIONAL;
    }

    public boolean allowsServerbound() {
        return this == SERVERBOUND || this == BIDIRECTIONAL;
    }
}
