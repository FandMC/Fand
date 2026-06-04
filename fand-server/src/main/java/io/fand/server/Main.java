package io.fand.server;

/**
 * Process entry point for the Fand runtime.
 */
public final class Main {

    private static final FandServer RUNTIME = new FandServer();

    private Main() {}

    public static FandServer runtime() {
        return RUNTIME;
    }

    public static void main(String[] args) {
        try {
            RUNTIME.start();
            net.minecraft.server.Main.main(args);
        } finally {
            RUNTIME.close();
        }
    }
}
