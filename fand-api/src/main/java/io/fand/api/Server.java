package io.fand.api;

import io.fand.api.command.CommandRegistry;
import io.fand.api.entity.Player;
import io.fand.api.event.EventBus;
import io.fand.api.lifecycle.LifecyclePhase;
import io.fand.api.performance.ServerPerformance;
import io.fand.api.permission.PermissionService;
import io.fand.api.plugin.PluginManager;
import io.fand.api.scheduler.Scheduler;
import io.fand.api.world.World;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Top-level handle to a running Fand server instance.
 *
 * <p>Obtain the singleton via {@link Fand#server()}. The instance is bound during
 * server bootstrap and remains valid for the lifetime of the JVM.
 *
 * <p>{@code Server} is an Adventure {@link ForwardingAudience} that forwards to
 * every online player. {@code server().sendMessage(component)} broadcasts to
 * everyone; {@code server().showTitle(title)} shows a title to everyone; etc.
 */
public interface Server extends ForwardingAudience {

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

    /** Current server tick performance snapshot. */
    ServerPerformance performance();

    /** Snapshot of all currently online players. The returned collection is a copy. */
    Collection<? extends Player> players();

    /** Looks up an online player by uuid. */
    Optional<? extends Player> player(UUID uniqueId);

    /** Looks up an online player by exact (case-sensitive) name. */
    Optional<? extends Player> player(String name);

    /** Snapshot of all loaded worlds. The returned collection is a copy. */
    Collection<? extends World> worlds();

    /** Looks up a loaded world by dimension key (e.g. {@code minecraft:overworld}). */
    Optional<? extends World> world(Key key);

    /** The default (overworld) world. Present once the server has finished loading. */
    Optional<? extends World> defaultWorld();

    /** Looks up a block type by its registry key. */
    Optional<? extends io.fand.api.block.BlockType> blockType(Key key);

    /** Looks up an item type by its registry key. */
    Optional<? extends io.fand.api.item.ItemType> itemType(Key key);

    /** Current lifecycle phase. */
    LifecyclePhase phase();

    /** Initiates an orderly shutdown. */
    void shutdown(@Nullable String reason);

    /**
     * Creates a new server-side {@link io.fand.api.inventory.Inventory} of
     * the given type, size, and title. Used by
     * {@link io.fand.api.inventory.Inventories} — plugins should usually
     * call that instead.
     *
     * @throws IllegalArgumentException if {@code size} is invalid for
     *         {@code type}, or if {@code type} is not standalone-openable
     */
    io.fand.api.inventory.Inventory createInventory(
            io.fand.api.inventory.InventoryType type,
            int size,
            net.kyori.adventure.text.Component title);
}
