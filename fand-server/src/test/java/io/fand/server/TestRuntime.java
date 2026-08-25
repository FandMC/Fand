package io.fand.server;

import io.fand.server.config.FandConfig;

public final class TestRuntime implements AutoCloseable {

    private final FandServer server;

    private TestRuntime(FandServer server) {
        this.server = server;
    }

    public static TestRuntime bind() {
        var server = new FandServer(new FandConfig(), TestRuntime.class.getClassLoader());
        Main.bind(server);
        return new TestRuntime(server);
    }

    @Override
    public void close() {
        try {
            server.close();
        } finally {
            Main.unbind(server);
        }
    }
}
