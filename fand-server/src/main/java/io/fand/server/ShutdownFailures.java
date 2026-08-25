package io.fand.server;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

final class ShutdownFailures {

    private final Logger logger;
    private @Nullable IllegalStateException combined;

    ShutdownFailures(Logger logger) {
        this.logger = java.util.Objects.requireNonNull(logger, "logger");
    }

    void run(String step, Runnable action) {
        java.util.Objects.requireNonNull(step, "step");
        java.util.Objects.requireNonNull(action, "action");
        try {
            action.run();
        } catch (Throwable failure) {
            logger.warn("Fand shutdown step '{}' failed", step, failure);
            if (combined == null) {
                combined = new IllegalStateException("Fand runtime shutdown failed");
            }
            combined.addSuppressed(failure);
        }
    }

    void throwIfPresent() {
        if (combined != null) {
            throw combined;
        }
    }
}
