package io.fand.server;

import org.jspecify.annotations.Nullable;

/**
 * Process entry point for the Fand runtime.
 */
public final class Main {

    private static volatile @Nullable FandServer runtime;

    private Main() {}

    public static FandServer runtime() {
        var local = runtime;
        if (local == null) {
            throw new IllegalStateException("Fand runtime has not been bootstrapped yet");
        }
        return local;
    }

    public static @Nullable FandServer runtimeOrNull() {
        return runtime;
    }

    static void bind(FandServer server) {
        synchronized (Main.class) {
            if (runtime != null) {
                throw new IllegalStateException("Fand runtime is already bootstrapped");
            }
            runtime = server;
        }
    }

    static void unbind(FandServer server) {
        synchronized (Main.class) {
            if (runtime == server) {
                runtime = null;
            }
        }
    }

    public static void main(String[] args) {
        var server = new FandServer();
        bind(server);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.close();
            } finally {
                unbind(server);
            }
        }, "Fand-Shutdown"));
        server.start();
        net.minecraft.server.Main.main(args);
        server.awaitMinecraftServerStop();
    }
}
