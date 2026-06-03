package io.fand.api;

import io.fand.api.event.EventBus;
import io.fand.api.plugin.PluginManager;
import io.fand.api.scheduler.Scheduler;
import org.jspecify.annotations.Nullable;

/**
 * Top-level handle to a running Fand server instance.
 *
 * <p>Obtain the singleton via {@link Fand#server()}. The instance is bound during
 * server bootstrap and remains valid for the lifetime of the JVM.
 */
public interface Server {

    /** Server brand identifier reported to clients. */
    String brand();

    /** Running Fand version (e.g. {@code 0.1.0-SNAPSHOT}). */
    String version();

    /** Minecraft protocol version this server implements. */
    String minecraftVersion();

    /** Plugin lifecycle and lookup. */
    PluginManager plugins();

    /** Global event dispatcher. */
    EventBus events();

    /** Main-thread and async task scheduler. */
    Scheduler scheduler();

    /** Currently online player count. */
    int onlinePlayers();

    /** Configured maximum simultaneous players, or {@code -1} for uncapped. */
    int maxPlayers();

    /** Initiates an orderly shutdown. */
    void shutdown(@Nullable String reason);
}
