package io.fand.api;

import io.fand.api.internal.FandRuntime;

/**
 * Read-only static accessor for the running {@link Server} instance.
 *
 * <p>Plugin code obtains the server through this class. Runtime lifecycle binding
 * is intentionally kept outside the stable plugin API.
 */
public final class Fand {

    private Fand() {}

    /** Returns the bound server. Throws if accessed before bootstrap completes. */
    public static Server server() {
        return FandRuntime.server();
    }
}
