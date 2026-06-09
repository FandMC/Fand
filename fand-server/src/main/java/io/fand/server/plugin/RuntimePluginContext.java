package io.fand.server.plugin;

import io.fand.api.command.CommandRegistry;
import io.fand.api.config.Configuration;
import io.fand.api.event.EventBus;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.permission.PermissionService;
import io.fand.api.plugin.PluginContext;
import io.fand.api.plugin.PluginDescriptor;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.scheduler.Scheduler;
import io.fand.api.scoreboard.ScoreboardService;
import io.fand.server.config.YamlConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;

public final class RuntimePluginContext implements PluginContext {

    private static final String CONFIG_RESOURCE = "config.yml";
    private static final String CONFIG_FILE = "config.yml";

    private final PluginDescriptor descriptor;
    private final Logger logger;
    private final EventBus events;
    private final PermissionService permissions;
    private final CommandRegistry commands;
    private final RecipeRegistry recipes;
    private final ScoreboardService scoreboard;
    private final PacketRegistry packets;
    private final Scheduler scheduler;
    private final Path dataDirectory;
    private final PluginResourceTracker resources;
    private final ClassLoader pluginClassLoader;
    private volatile YamlConfiguration config;

    public RuntimePluginContext(
            PluginDescriptor descriptor,
            Logger logger,
            EventBus events,
            PermissionService permissions,
            CommandRegistry commands,
            RecipeRegistry recipes,
            ScoreboardService scoreboard,
            PacketRegistry packets,
            Scheduler scheduler,
            Path dataDirectory,
            PluginResourceTracker resources,
            ClassLoader pluginClassLoader
    ) {
        this.descriptor = descriptor;
        this.logger = logger;
        this.events = events;
        this.permissions = permissions;
        this.commands = commands;
        this.recipes = recipes;
        this.scoreboard = scoreboard;
        this.packets = packets;
        this.scheduler = scheduler;
        this.dataDirectory = dataDirectory;
        this.resources = resources;
        this.pluginClassLoader = pluginClassLoader;
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
    public PermissionService permissions() {
        return permissions;
    }

    @Override
    public CommandRegistry commands() {
        return commands;
    }

    @Override
    public RecipeRegistry recipes() {
        return recipes;
    }

    @Override
    public ScoreboardService scoreboard() {
        return scoreboard;
    }

    @Override
    public PacketRegistry packets() {
        return packets;
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

    @Override
    public Configuration config() {
        var existing = config;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (config != null) {
                return config;
            }
            var configFile = dataDirectory().resolve(CONFIG_FILE);
            try (InputStream defaults = pluginClassLoader.getResourceAsStream(CONFIG_RESOURCE)) {
                config = YamlConfiguration.loadOrCopyDefault(configFile, defaults);
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to load bundled defaults for plugin " + descriptor.id(), ex);
            }
            return config;
        }
    }

    @Override
    public Configuration reloadConfig() {
        var current = config();
        current.reload();
        return current;
    }

    public void close() {
        resources.close();
    }
}
