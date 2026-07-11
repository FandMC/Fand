package io.fand.api.internal;

import io.fand.api.Server;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Internal lifecycle bridge used by the server runtime to bind the public
 * {@link Server} accessor.
 */
@ApiStatus.Internal
public final class FandRuntime {

    private static volatile @Nullable Server instance;

    private FandRuntime() {}

    public static Server server() {
        Server local = instance;
        if (local == null) {
            throw new IllegalStateException("Fand server has not been bootstrapped yet");
        }
        return local;
    }

    public static void bind(Server server) {
        Objects.requireNonNull(server, "server");
        synchronized (FandRuntime.class) {
            if (instance != null) {
                throw new IllegalStateException("Fand server is already bound");
            }
            instance = server;
        }
    }

    public static void unbind(Server server) {
        Objects.requireNonNull(server, "server");
        synchronized (FandRuntime.class) {
            if (instance == server) {
                instance = null;
            }
        }
    }
}
