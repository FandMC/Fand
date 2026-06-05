package io.fand.api.lifecycle;

import io.fand.api.Server;
import io.fand.api.event.Event;

/**
 * Fired immediately before plugins are enabled. The server has finished its
 * vanilla initialization and is ready to accept registrations from plugins.
 */
public final class ServerStartingEvent implements Event {

    private final Server server;

    public ServerStartingEvent(Server server) {
        this.server = server;
    }

    public Server server() {
        return server;
    }
}
