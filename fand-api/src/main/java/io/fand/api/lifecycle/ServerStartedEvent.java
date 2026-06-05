package io.fand.api.lifecycle;

import io.fand.api.Server;
import io.fand.api.event.Event;

/**
 * Fired after every plugin has been enabled and the server is running.
 */
public final class ServerStartedEvent implements Event {

    private final Server server;

    public ServerStartedEvent(Server server) {
        this.server = server;
    }

    public Server server() {
        return server;
    }
}
