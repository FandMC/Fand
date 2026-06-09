package io.fand.api;

import io.fand.api.command.CommandRegistry;
import io.fand.api.entity.EntityKey;
import io.fand.api.entity.Player;
import io.fand.api.event.EventBus;
import io.fand.api.lifecycle.LifecyclePhase;
import io.fand.api.performance.ServerPerformance;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.permission.PermissionService;
import io.fand.api.plugin.PluginManager;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.scheduler.Scheduler;
import io.fand.api.world.World;
import io.fand.api.world.WorldTemplate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

    /** Global recipe registry. Registration and removal marshal to the server thread. */
    RecipeRegistry recipes();

    /** Global packet registry. Prefer {@link io.fand.api.plugin.PluginContext#packets()} for plugin-owned registrations. */
    PacketRegistry packets();

    /** Main-thread and async task scheduler. */
    Scheduler scheduler();

    /** Currently online player count. */
    int onlinePlayers();

    /** Configured maximum simultaneous players, or {@code -1} for uncapped. */
    int maxPlayers();

    /** Latest published server tick performance snapshot. */
    ServerPerformance performance();

    /** Snapshot of all currently online players. The returned collection is a copy. */
    Collection<? extends Player> players();

    /** Looks up an online player by uuid. */
    Optional<? extends Player> player(UUID uniqueId);

    /** Looks up an online player by exact (case-sensitive) name. */
    Optional<? extends Player> player(String name);

    /** Looks up a loaded entity by uuid across all worlds, including players. */
    Optional<? extends io.fand.api.entity.Entity> entity(UUID uniqueId);

    /** Snapshot of all loaded worlds. The returned collection is a copy. */
    Collection<? extends World> worlds();

    /** Looks up a loaded world by dimension key (e.g. {@code minecraft:overworld}). */
    Optional<? extends World> world(Key key);

    /** The default (overworld) world. Present once the server has finished loading. */
    Optional<? extends World> defaultWorld();

    /**
     * Creates and loads a dynamic world from a vanilla generation template.
     *
     * <p>The operation marshals to the server thread. The returned future fails
     * when the key is already loaded, the server is not running, or the selected
     * template is unavailable in the active dimension registry.
     */
    CompletableFuture<? extends World> createWorld(Key key, WorldTemplate template);

    /**
     * Saves and unloads a dynamic world. Vanilla base dimensions cannot be
     * unloaded, and worlds with players are rejected.
     *
     * <p>The operation marshals to the server thread and fires
     * {@link io.fand.api.event.world.WorldUnloadEvent} before the level is
     * removed.
     */
    CompletableFuture<Boolean> unloadWorld(Key key);

    /** Convenience for {@code unloadWorld(world.key())}. */
    default CompletableFuture<Boolean> unloadWorld(World world) {
        return unloadWorld(world.key());
    }

    /** Looks up a block type by its registry key. */
    Optional<? extends io.fand.api.block.BlockType> blockType(Key key);

    /** Looks up an item type by its registry key. */
    Optional<? extends io.fand.api.item.ItemType> itemType(Key key);

    /** Looks up an entity type by its registry key. */
    Optional<? extends io.fand.api.entity.EntityType> entityType(Key key);

    /** Convenience overload for generated vanilla entity keys. */
    default Optional<? extends io.fand.api.entity.EntityType> entityType(EntityKey key) {
        return entityType(key.key());
    }

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
