package io.fand.api;

import io.fand.api.command.CommandRegistry;
import io.fand.api.entity.Player;
import io.fand.api.event.EventBus;
import io.fand.api.lifecycle.LifecyclePhase;
import io.fand.api.permission.PermissionService;
import io.fand.api.plugin.PluginManager;
import io.fand.api.scheduler.Scheduler;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
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

    /** Global permission service. */
    PermissionService permissions();

    /** Global command registry. */
    CommandRegistry commands();

    /** Main-thread and async task scheduler. */
    Scheduler scheduler();

    /** Currently online player count. */
    int onlinePlayers();

    /** Configured maximum simultaneous players, or {@code -1} for uncapped. */
    int maxPlayers();

    /** Snapshot of all currently online players. The returned collection is a copy. */
    Collection<? extends Player> players();

    /** Looks up an online player by uuid. */
    Optional<? extends Player> player(UUID uniqueId);

    /** Looks up an online player by exact (case-sensitive) name. */
    Optional<? extends Player> player(String name);

    /** Current lifecycle phase. */
    LifecyclePhase phase();

    /** Initiates an orderly shutdown. */
    void shutdown(@Nullable String reason);
}
