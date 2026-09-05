package io.fand.server.tick;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Server-owned workers. World phases join their jobs before publication, reload or shutdown. */
public final class RegionSimulationWorkers implements AutoCloseable {
    private final ExecutorService executor;
    private final int parallelism;

    public RegionSimulationWorkers(int configured) {
        if (configured < 0) {
            throw new IllegalArgumentException("Region worker count must not be negative");
        }
        this.parallelism = configured == 0
                ? Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() / 2)) : configured;
        this.executor = Executors.newFixedThreadPool(this.parallelism,
                Thread.ofPlatform().name("Fand Region #", 1).daemon(true).factory());
    }

    public int parallelism() {
        return this.parallelism;
    }

    public Executor executor() {
        return this.executor;
    }

    @Override
    public void close() {
        this.executor.close();
    }
}
