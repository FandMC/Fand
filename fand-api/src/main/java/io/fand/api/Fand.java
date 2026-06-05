package io.fand.api;

import java.util.Objects;

/**
 * Static accessor for the running {@link Server} instance.
 *
 * <p>The server runtime calls {@link #bind(Server)} exactly once during bootstrap.
 * Plugin code should treat the accessor as read-only.
 */
public final class Fand {

    private static volatile @org.jspecify.annotations.Nullable Server instance;

    private Fand() {}

    /** Returns the bound server. Throws if accessed before bootstrap completes. */
    public static Server server() {
        Server local = instance;
        if (local == null) {
            throw new IllegalStateException("Fand server has not been bootstrapped yet");
        }
        return local;
    }

    /**
     * Binds the server instance. Intended to be invoked exactly once by the runtime.
     *
     * @throws IllegalStateException if already bound
     */
    public static void bind(Server server) {
        Objects.requireNonNull(server, "server");
        synchronized (Fand.class) {
            if (instance != null) {
                throw new IllegalStateException("Fand server is already bound");
            }
            instance = server;
        }
    }

    /**
     * Releases the binding established by {@link #bind(Server)}. Intended to be
     * invoked exactly once by the runtime during shutdown. Subsequent
     * {@link #server()} calls throw until {@link #bind(Server)} runs again.
     */
    public static void unbind(Server server) {
        Objects.requireNonNull(server, "server");
        synchronized (Fand.class) {
            if (instance == server) {
                instance = null;
            }
        }
    }
}
