package io.fand.server;

import io.fand.api.Fand;
import io.fand.api.Server;
import io.fand.api.event.EventBus;
import io.fand.api.plugin.PluginManager;
import io.fand.api.scheduler.Scheduler;
import io.fand.server.event.StandardEventBus;
import io.fand.server.plugin.EmptyPluginManager;
import io.fand.server.scheduler.StandardScheduler;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FandServer implements Server, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandServer.class);

    private final StandardEventBus events = new StandardEventBus();
    private final StandardScheduler scheduler = new StandardScheduler();
    private final PluginManager plugins = new EmptyPluginManager();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<MinecraftServer> minecraftServer = new AtomicReference<>();

    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Fand server is already started");
        }
        Fand.bind(this);
        LOGGER.info("Starting Fand {} for Minecraft {}", version(), minecraftVersion());
    }

    public void attach(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (!minecraftServer.compareAndSet(null, server)) {
            throw new IllegalStateException("Minecraft server is already attached");
        }
    }

    public void tickScheduler() {
        scheduler.tick();
    }

    @Override
    public String brand() {
        return "Fand";
    }

    @Override
    public String version() {
        return BuildInfo.VERSION;
    }

    @Override
    public String minecraftVersion() {
        return BuildInfo.MINECRAFT_VERSION;
    }

    @Override
    public PluginManager plugins() {
        return plugins;
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
    public int onlinePlayers() {
        var server = minecraftServer.get();
        return server == null ? 0 : server.getPlayerCount();
    }

    @Override
    public int maxPlayers() {
        var server = minecraftServer.get();
        return server == null ? -1 : server.getMaxPlayers();
    }

    @Override
    public void shutdown(@Nullable String reason) {
        LOGGER.info("Shutdown requested: {}", reason == null ? "<no reason>" : reason);
        var server = minecraftServer.get();
        if (server == null) {
            close();
            return;
        }
        server.halt(false);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        scheduler.close();
        LOGGER.info("Fand runtime stopped");
    }
}
