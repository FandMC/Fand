package io.fand.server;

import io.fand.api.Fand;
import io.fand.api.Server;
import io.fand.api.command.CommandRegistry;
import io.fand.api.customblock.CustomBlockRegistry;
import io.fand.api.customitem.CustomItemRegistry;
import io.fand.api.event.EventBus;
import io.fand.api.event.world.WorldLoadEvent;
import io.fand.api.event.world.WorldUnloadEvent;
import io.fand.api.gui.GuiService;
import io.fand.api.lifecycle.LifecyclePhase;
import io.fand.api.lifecycle.ServerStartedEvent;
import io.fand.api.lifecycle.ServerStartingEvent;
import io.fand.api.lifecycle.ServerStoppingEvent;
import io.fand.api.permission.PermissionService;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.player.PlayerAccessService;
import io.fand.api.plugin.PluginManager;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.scheduler.Scheduler;
import io.fand.api.scoreboard.ScoreboardService;
import io.fand.api.world.World;
import io.fand.api.world.WorldCreateOptions;
import io.fand.api.world.WorldTemplate;
import io.fand.server.block.FandCustomBlockRegistry;
import io.fand.server.command.BuiltinCommands;
import io.fand.server.chunk.ChunkSendScheduler;
import io.fand.server.chunk.ChunkTrackingMetrics;
import io.fand.server.command.CommandManager;
import io.fand.server.config.ConfigReloadResult;
import io.fand.server.config.ConfigReloader;
import io.fand.server.config.FandConfig;
import io.fand.server.entity.EntityRegistry;
import io.fand.server.entity.PlayerRegistry;
import io.fand.server.event.EventDispatcher;
import io.fand.server.gui.FandGuiService;
import io.fand.server.item.FandCustomItemRegistry;
import io.fand.server.network.ProxyForwardingSettings;
import io.fand.server.network.packet.PacketRegistryImpl;
import io.fand.server.permission.PermissionManager;
import io.fand.server.performance.ServerPerformanceTracker;
import io.fand.server.player.FandPlayerAccessService;
import io.fand.server.plugin.PluginRuntime;
import io.fand.server.recipe.FandRecipeRegistry;
import io.fand.server.scheduler.TaskScheduler;
import io.fand.server.scoreboard.FandScoreboardService;
import io.fand.server.world.WorldRegistry;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FandServer implements Server, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandServer.class);

    private final Path configPath;
    private final EventDispatcher events;
    private final PermissionManager permissions;
    private final CommandManager commands;
    private final TaskScheduler scheduler;
    private final ChunkSendScheduler chunks;
    private final FandRecipeRegistry recipes;
    private final FandScoreboardService scoreboard;
    private final PacketRegistryImpl packets;
    private final FandCustomItemRegistry customItems;
    private final FandCustomBlockRegistry customBlocks;
    private final FandGuiService guis;
    private final PluginRuntime plugins;
    private final PlayerRegistry players;
    private final FandPlayerAccessService playerAccess;
    private final ServerPerformanceTracker performance;
    private final ConfigReloader configReloader;
    private final ProxyForwardingSettings proxyForwarding;
    private final io.fand.server.console.gui.GuiThemeService guiThemes;
    private final AtomicReference<WorldRegistry> worlds = new AtomicReference<>();
    private final AtomicReference<EntityRegistry> entities = new AtomicReference<>();
    private final AtomicReference<FandConfig> config;
    private final AtomicReference<LifecyclePhase> phase = new AtomicReference<>(LifecyclePhase.BOOTSTRAP);
    private final AtomicReference<MinecraftServer> minecraftServer = new AtomicReference<>();

    public FandServer() {
        this(Path.of("fand.yml"), FandConfig.load(Path.of("fand.yml")), Main.class.getClassLoader());
    }

    FandServer(FandConfig config, ClassLoader parentClassLoader) {
        this(Path.of("fand.yml"), config, parentClassLoader);
    }

    FandServer(Path configPath, FandConfig initialConfig, ClassLoader parentClassLoader) {
        this.configPath = configPath;
        this.config = new AtomicReference<>(initialConfig);
        this.proxyForwarding = ProxyForwardingSettings.fromConfig(initialConfig);
        this.guiThemes = new io.fand.server.console.gui.GuiThemeService(
                io.fand.server.console.gui.GuiTheme.fromConfig(initialConfig.console.gui.theme));
        this.events = new EventDispatcher();
        this.permissions = new PermissionManager(events);
        this.commands = new CommandManager(permissions);
        registerBuiltinCommands();
        this.scheduler = new TaskScheduler(initialConfig.scheduler.asyncThreads);
        this.chunks = new ChunkSendScheduler(initialConfig.chunks);
        this.recipes = new FandRecipeRegistry();
        this.scoreboard = new FandScoreboardService(minecraftServer::get);
        this.packets = new PacketRegistryImpl();
        this.customItems = new FandCustomItemRegistry();
        this.customBlocks = new FandCustomBlockRegistry(events, customItems);
        this.guis = new FandGuiService(events);
        this.players = new PlayerRegistry(permissions);
        this.playerAccess = new FandPlayerAccessService(minecraftServer::get);
        this.performance = new ServerPerformanceTracker();
        var pluginDirectory = Path.of(initialConfig.plugins.directory);
        this.plugins = new PluginRuntime(
                pluginDirectory,
                pluginDirectory,
                parentClassLoader,
                commands,
                events,
                permissions,
                scheduler,
                recipes,
                scoreboard,
                packets,
                customItems,
                customBlocks,
                guis,
                ConfigReloader.toPluginOptions(initialConfig)
        );
        this.configReloader = new ConfigReloader(configPath, config, plugins, scheduler, chunks, guiThemes);
    }

    /**
     * Binds the API singleton and runs plugin discovery + onLoad. Called before
     * the vanilla server is constructed so plugins observe a coherent runtime
     * before any world data exists.
     */
    public void start() {
        if (!phase.compareAndSet(LifecyclePhase.BOOTSTRAP, LifecyclePhase.LOADED)) {
            throw new IllegalStateException("Fand runtime already started, current phase: " + phase.get());
        }
        Fand.bind(this);
        plugins.loadPlugins();
        LOGGER.info("Loaded Fand {} for Minecraft {}", version(), minecraftVersion());
    }

    /**
     * Runs after the vanilla server is initialized but before it begins ticking.
     * Fires {@link ServerStartingEvent}, enables plugins, then fires
     * {@link ServerStartedEvent}.
     */
    public void enable() {
        if (!phase.compareAndSet(LifecyclePhase.LOADED, LifecyclePhase.STARTING)) {
            throw new IllegalStateException("enable() requires LOADED phase, was: " + phase.get());
        }
        try {
            events.fire(new ServerStartingEvent(this));
            plugins.enablePlugins();
            fireWorldLoadEvents();
            recipes.applyLoadedRecipes();
            phase.set(LifecyclePhase.RUNNING);
            events.fire(new ServerStartedEvent(this));
        } catch (Throwable failure) {
            LOGGER.error("Fand enable() failed", failure);
            throw failure;
        }
    }

    public ConfigReloadResult reloadConfig() {
        return configReloader.reload();
    }

    public ProxyForwardingSettings proxyForwarding() {
        return proxyForwarding;
    }

    public boolean consoleGuiEnabled() {
        return config.get().console.gui.enabled;
    }

    public io.fand.server.console.gui.GuiThemeService guiThemes() {
        return guiThemes;
    }

    public void attach(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (!minecraftServer.compareAndSet(null, server)) {
            throw new IllegalStateException("Minecraft server is already attached");
        }
        var registry = new WorldRegistry(server, players, scheduler);
        worlds.set(registry);
        entities.set(registry.entityRegistry());
        players.bindWorldRegistry(registry);
        players.bindWorldResolver(registry::wrap);
        io.fand.server.item.FandItemStacks.useRegistries(server.registryAccess());
        recipes.bind(server);
    }

    /**
     * Blocks until the attached vanilla server thread terminates. Called by
     * Main after net.minecraft.server.Main.main returns so the launcher
     * (Fandclip) does not close the plugin classloader while the server is
     * still running.
     */
    public void awaitMinecraftServerStop() {
        var server = minecraftServer.get();
        if (server == null) {
            return;
        }
        try {
            server.getRunningThread().join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public void tickScheduler() {
        if (phase.get() == LifecyclePhase.STOPPING || phase.get() == LifecyclePhase.STOPPED) {
            return;
        }
        scheduler.tick();
        recipes.tick();
        for (var world : worlds()) {
            customBlocks.tick(world);
        }
    }

    public ChunkSendScheduler chunkSendScheduler() {
        return chunks;
    }

    public ChunkTrackingMetrics chunkTrackingMetrics() {
        return chunks.metrics();
    }

    public void recordTick(long tickStartNanos, long tickDurationNanos) {
        performance.recordTick(tickStartNanos, tickDurationNanos);
    }

    public void recordTick(long tickStartNanos, long tickDurationNanos, long taskExecutionNanos) {
        performance.recordTick(tickStartNanos, tickDurationNanos, taskExecutionNanos);
    }

    @Override
    public String brand() {
        return config.get().identity.brand;
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
    public PermissionService permissions() {
        return permissions;
    }

    public CommandManager commandManager() {
        return commands;
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
    public CustomItemRegistry customItems() {
        return customItems;
    }

    @Override
    public CustomBlockRegistry customBlocks() {
        return customBlocks;
    }

    public FandCustomBlockRegistry customBlockRegistry() {
        return customBlocks;
    }

    @Override
    public GuiService guis() {
        return guis;
    }

    public PacketRegistryImpl packetRegistry() {
        return packets;
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
    public io.fand.api.performance.ServerPerformance performance() {
        return performance.snapshot();
    }

    @Override
    public Collection<? extends io.fand.api.entity.Player> players() {
        return players.snapshot();
    }

    @Override
    public PlayerAccessService playerAccess() {
        return playerAccess;
    }

    @Override
    public Iterable<? extends net.kyori.adventure.audience.Audience> audiences() {
        return players.snapshot();
    }

    @Override
    public Optional<? extends io.fand.api.entity.Player> player(UUID uniqueId) {
        return players.find(uniqueId);
    }

    @Override
    public Optional<? extends io.fand.api.entity.Player> player(String name) {
        return players.findByName(name);
    }

    @Override
    public Optional<? extends io.fand.api.entity.Entity> entity(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        var player = players.find(uniqueId);
        if (player.isPresent()) {
            return player;
        }
        var registry = worlds.get();
        if (registry == null) {
            return Optional.empty();
        }
        for (var world : registry.snapshot()) {
            var found = world.entity(uniqueId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    public PlayerRegistry playerRegistry() {
        return players;
    }

    @Override
    public Collection<? extends io.fand.api.world.World> worlds() {
        var registry = worlds.get();
        return registry == null ? List.of() : registry.snapshot();
    }

    @Override
    public Optional<? extends io.fand.api.world.World> world(Key key) {
        var registry = worlds.get();
        return registry == null ? Optional.empty() : registry.find(key);
    }

    @Override
    public Optional<? extends io.fand.api.world.World> defaultWorld() {
        var registry = worlds.get();
        return registry == null ? Optional.empty() : registry.defaultWorld();
    }

    @Override
    public CompletableFuture<World> createWorld(Key key, WorldTemplate template) {
        return createWorld(key, WorldCreateOptions.of(template));
    }

    @Override
    public CompletableFuture<World> createWorld(Key key, WorldCreateOptions options) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(options, "options");
        var server = minecraftServer.get();
        var registry = worlds.get();
        if (server == null || registry == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minecraft server is not attached"));
        }
        if (server.isStopped()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minecraft server is stopping"));
        }
        return server.submit(() -> {
            var level = server.fand$createDynamicLevel(dimensionKey(key), levelStem(server, options));
            World world = registry.wrap(level);
            events.fire(new WorldLoadEvent(world));
            return world;
        });
    }

    @Override
    public CompletableFuture<Boolean> unloadWorld(Key key) {
        Objects.requireNonNull(key, "key");
        var server = minecraftServer.get();
        var registry = worlds.get();
        if (server == null || registry == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minecraft server is not attached"));
        }
        if (server.isStopped()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minecraft server is stopping"));
        }
        return server.submit(() -> {
            var world = registry.find(key).map(FandServer::requireFandWorld).orElse(null);
            if (world == null) {
                return false;
            }
            events.fire(new WorldUnloadEvent(world));
            boolean unloaded = server.fand$unloadDynamicLevel(dimensionKey(key));
            if (unloaded) {
                registry.forget(world);
            }
            return unloaded;
        });
    }

    public Optional<WorldRegistry> worldRegistry() {
        return Optional.ofNullable(worldRegistryOrNull());
    }

    public @Nullable WorldRegistry worldRegistryOrNull() {
        return worlds.get();
    }

    public Optional<EntityRegistry> entityRegistry() {
        return Optional.ofNullable(entityRegistryOrNull());
    }

    public @Nullable EntityRegistry entityRegistryOrNull() {
        return entities.get();
    }

    @Override
    public Optional<? extends io.fand.api.block.BlockType> blockType(Key key) {
        Objects.requireNonNull(key, "key");
        var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(key.namespace(), key.value());
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(id)
                .map(io.fand.server.block.FandBlockType::of);
    }

    @Override
    public Optional<? extends io.fand.api.item.ItemType> itemType(Key key) {
        Objects.requireNonNull(key, "key");
        var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(key.namespace(), key.value());
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id)
                .map(io.fand.server.item.FandItemType::of);
    }

    @Override
    public Optional<? extends io.fand.api.entity.EntityType> entityType(Key key) {
        Objects.requireNonNull(key, "key");
        var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(key.namespace(), key.value());
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                .map(io.fand.server.entity.FandEntityType::of);
    }

    @Override
    public LifecyclePhase phase() {
        return phase.get();
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
    public io.fand.api.inventory.Inventory createInventory(
            io.fand.api.inventory.InventoryType type,
            int size,
            net.kyori.adventure.text.Component title) {
        java.util.Objects.requireNonNull(type, "type");
        java.util.Objects.requireNonNull(title, "title");
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0, got " + size);
        }
        if (type == io.fand.api.inventory.InventoryType.PLAYER
                || type == io.fand.api.inventory.InventoryType.UNKNOWN) {
            throw new IllegalArgumentException("Cannot create a standalone inventory of type " + type);
        }
        var probe = io.fand.server.inventory.OpenableContainers.build(type, size);
        if (probe == null) {
            throw new IllegalArgumentException(
                    "InventoryType " + type + " cannot be opened standalone — needs a backing block");
        }
        int resolvedSize = probe.container().getContainerSize();
        return new io.fand.server.inventory.FandInventory(type, resolvedSize, title);
    }

    @Override
    public void close() {
        LifecyclePhase current;
        while (true) {
            current = phase.get();
            if (current == LifecyclePhase.STOPPING || current == LifecyclePhase.STOPPED) {
                return;
            }
            if (phase.compareAndSet(current, LifecyclePhase.STOPPING)) {
                break;
            }
        }

        if (current == LifecyclePhase.RUNNING || current == LifecyclePhase.STARTING) {
            try {
                events.fire(new ServerStoppingEvent(this, null));
            } catch (RuntimeException failure) {
                LOGGER.warn("ServerStoppingEvent listener failed", failure);
            }
            fireWorldUnloadEvents();
        }
        plugins.disablePlugins();
        plugins.close();
        packets.close();
        guis.close();
        chunks.close();
        scheduler.close();
        guiThemes.close();
        performance.close();
        Fand.unbind(this);
        phase.set(LifecyclePhase.STOPPED);
        LOGGER.info("Fand runtime stopped");
    }

    private void registerBuiltinCommands() {
        BuiltinCommands.registerAll(commands, this);
    }

    private static ResourceKey<Level> dimensionKey(Key key) {
        return ResourceKey.create(Registries.DIMENSION, identifier(key));
    }

    private static ResourceKey<LevelStem> templateKey(WorldTemplate template) {
        return ResourceKey.create(Registries.LEVEL_STEM, identifier(template.key()));
    }

    private static LevelStem levelStem(MinecraftServer server, WorldCreateOptions options) {
        var template = templateKey(options.template());
        if (options.isVoidWorld()) {
            return server.fand$voidLevelStem(template);
        }
        var generator = options.generator().orElse(null);
        var dimensions = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM);
        var base = dimensions.getValue(template);
        if (base == null) {
            throw new IllegalArgumentException("World template is not available: " + template.identifier());
        }
        if (generator == null) {
            return base;
        }
        var biome = server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        return new LevelStem(base.type(), new io.fand.server.world.FandWorldGeneratorSource(biome, generator));
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static io.fand.server.world.FandWorld requireFandWorld(io.fand.api.world.World world) {
        if (world instanceof io.fand.server.world.FandWorld fandWorld) {
            return fandWorld;
        }
        throw new IllegalArgumentException("World is not owned by this server: " + world.key().asString());
    }

    private void fireWorldLoadEvents() {
        for (var world : worlds()) {
            events.fire(new WorldLoadEvent(world));
        }
    }

    private void fireWorldUnloadEvents() {
        for (var world : worlds()) {
            try {
                events.fire(new WorldUnloadEvent(world));
            } catch (RuntimeException failure) {
                LOGGER.warn("WorldUnloadEvent listener failed for {}", world.key().asString(), failure);
            }
        }
    }
}
