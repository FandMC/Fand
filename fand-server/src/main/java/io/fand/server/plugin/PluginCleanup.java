package io.fand.server.plugin;

import org.jspecify.annotations.Nullable;

final class PluginCleanup {

    private final String message;
    private @Nullable PluginLoadException failure;

    PluginCleanup(String message) {
        this.message = message;
    }

    void run(Action action) {
        try {
            action.run();
        } catch (Throwable cause) {
            // A broken plugin callback must not prevent releasing other resources.
            if (failure == null) {
                failure = new PluginLoadException(message, cause);
            } else {
                failure.addSuppressed(cause);
            }
        }
    }

    void throwIfFailed() {
        if (failure != null) {
            throw failure;
        }
    }

    @FunctionalInterface
    interface Action {
        void run() throws Exception;
    }
}
