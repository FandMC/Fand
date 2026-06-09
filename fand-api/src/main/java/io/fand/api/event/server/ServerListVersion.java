package io.fand.api.event.server;

import java.util.Objects;

/**
 * Version text and protocol shown in the multiplayer server list.
 */
public record ServerListVersion(String name, int protocol) {

    public ServerListVersion {
        name = Objects.requireNonNull(name, "name").trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (protocol < 0) {
            throw new IllegalArgumentException("protocol must be >= 0");
        }
    }
}
