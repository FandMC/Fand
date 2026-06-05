package io.fand.api.lifecycle;

import io.fand.api.Server;
import io.fand.api.event.Event;
import org.jspecify.annotations.Nullable;

/**
 * Fired when shutdown is initiated, before plugins are disabled.
 */
public final class ServerStoppingEvent implements Event {

    private final Server server;
    private final @Nullable String reason;

    public ServerStoppingEvent(Server server, @Nullable String reason) {
        this.server = server;
        this.reason = reason;
    }

    public Server server() {
        return server;
    }

    public @Nullable String reason() {
        return reason;
    }
}
