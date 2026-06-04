package io.fand.server.plugin;

import io.fand.api.event.EventBus;
import io.fand.api.plugin.PluginContext;
import io.fand.api.plugin.PluginDescriptor;
import io.fand.api.scheduler.Scheduler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;

public final class RuntimePluginContext implements PluginContext {

    private final PluginDescriptor descriptor;
    private final Logger logger;
    private final EventBus events;
    private final Scheduler scheduler;
    private final Path dataDirectory;
    private final PluginResourceTracker resources;

    public RuntimePluginContext(
            PluginDescriptor descriptor,
            Logger logger,
            EventBus events,
            Scheduler scheduler,
            Path dataDirectory,
            PluginResourceTracker resources
    ) {
        this.descriptor = descriptor;
        this.logger = logger;
        this.events = events;
        this.scheduler = scheduler;
        this.dataDirectory = dataDirectory;
        this.resources = resources;
    }

    @Override
    public PluginDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public EventBus events() {
        return events;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public Path dataDirectory() {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to create data directory for plugin " + descriptor.id(), ex);
        }
        return dataDirectory;
    }

    public void close() {
        resources.close();
    }
}
