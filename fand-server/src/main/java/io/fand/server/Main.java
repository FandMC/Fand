package io.fand.server;

/**
 * Process entry point for the Fand runtime.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        net.minecraft.server.Main.main(args);
    }
}
