package io.fand.server.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import io.fand.api.advancement.AdvancementRegistry;
import io.fand.api.auth.LoginAuthenticationService;
import io.fand.api.bossbar.BossBarService;
import io.fand.api.command.CommandInfo;
import io.fand.api.command.CommandRegistry;
import io.fand.api.block.custom.CustomBlockRegistry;
import io.fand.api.item.custom.CustomItemRegistry;
import io.fand.api.datapack.DataPackService;
import io.fand.api.enchantment.EnchantmentRegistry;
import io.fand.api.event.EventBus;
import io.fand.api.gamerule.GameRuleService;
import io.fand.api.gui.GuiService;
import io.fand.api.hologram.HologramService;
import io.fand.api.integration.ExternalIntegrationStrategy;
import io.fand.api.lifecycle.PluginDisableEvent;
import io.fand.api.lifecycle.PluginEnableEvent;
import io.fand.api.loot.LootTableService;
import io.fand.api.map.MapService;
import io.fand.api.messaging.PluginMessaging;
import io.fand.api.nms.NmsService;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.placeholder.PlaceholderService;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionService;
import io.fand.api.player.SimulatedPlayerService;
import io.fand.api.region.RegionService;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginDescriptor;
import io.fand.api.plugin.PluginManager;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.resourcepack.ResourcePackService;
import io.fand.api.scheduler.Scheduler;
import io.fand.api.scoreboard.ScoreboardService;
import io.fand.api.service.ServiceRegistry;
import io.fand.api.structure.StructureService;
import io.fand.api.tablist.TabListService;
import io.fand.server.recipe.FandRecipeRegistry;
import io.fand.server.block.FandCustomBlockRegistry;
import io.fand.server.gui.FandGuiService;
import io.fand.server.item.FandCustomItemRegistry;
import io.fand.server.messaging.FandPluginMessaging;
import io.fand.server.network.packet.PacketRegistryImpl;
import io.fand.server.scoreboard.FandScoreboardService;
import io.fand.server.service.FandServiceRegistry;
import io.fand.server.text.FandMiniMessageService;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PluginRuntime implements PluginManager, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginRuntime.class);
    private static final Gson GSON = new Gson();
    private static final Pattern PLUGIN_ID = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern PERMISSION_NODE = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*(?:\\.[a-z0-9]+(?:[._-][a-z0-9]+)*)*(?:\\.\\*)?");
    private static final String DESCRIPTOR_PATH = "fand-plugin.json";
    private static final Map<String, String> FOREIGN_PLUGIN_DESCRIPTORS = Map.ofEntries(
            Map.entry("plugin.yml", "Bukkit/Spigot/Paper"),
            Map.entry("paper-plugin.yml", "Paper"),
            Map.entry("bungee.yml", "BungeeCord"),
            Map.entry("velocity-plugin.json", "Velocity"),
            Map.entry("fabric.mod.json", "Fabric"),
            Map.entry("quilt.mod.json", "Quilt"),
            Map.entry("META-INF/mods.toml", "Forge"),
            Map.entry("META-INF/neoforge.mods.toml", "NeoForge"),
            Map.entry("META-INF/sponge_plugins.json", "Sponge"),
            Map.entry("mcmod.info", "Legacy Forge")
    );

    private final Path pluginsDirectory;
    private final Path dataDirectoryRoot;
    private final PluginLibraryResolver libraryResolver;
    private final ClassLoader parentClassLoader;
    private final CommandRegistry commandRegistry;
    private final EventBus eventBus;
    private final PermissionService permissions;
    private final RecipeRegistry recipeRegistry;
    private final LootTableService lootTableService;
    private final AdvancementRegistry advancementRegistry;
    private final EnchantmentRegistry enchantmentRegistry;
    private final StructureService structureService;
    private final MapService mapService;
    private final BossBarService bossBarService;
    private final HologramService hologramService;
    private final TabListService tabListService;
    private final PluginTabListVisibilityRegistry tabListVisibilityRegistry = new PluginTabListVisibilityRegistry();
    private final SimulatedPlayerService simulatedPlayerService;
    private final PlaceholderService placeholderService;
    private final ScoreboardService scoreboardService;
    private final PacketRegistry packetRegistry;
    private final PluginMessaging pluginMessaging;
    private volatile GameRuleService gameRuleService = GameRuleService.empty();
    private final RegionService regionService;
    private final DataPackService dataPackService;
    private final ResourcePackService resourcePackService;
    private volatile ExternalIntegrationStrategy integrationStrategy = ExternalIntegrationStrategy.empty();
    private volatile ServiceRegistry serviceRegistry = new FandServiceRegistry();
    private volatile NmsService nmsService = NmsService.empty();
    private volatile LoginAuthenticationService loginAuthenticationService = LoginAuthenticationService.empty();
    private final CustomItemRegistry customItemRegistry;
    private final CustomBlockRegistry customBlockRegistry;
    private final GuiService guiService;
    private final boolean closeGuiService;
    private final Scheduler scheduler;
    private volatile Options options;
    private final Object lifecycleLock = new Object();
    private final ConcurrentHashMap<String, LoadedPlugin> loadedPlugins = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StatusEntry> statusEntries = new ConcurrentHashMap<>();
    private final Set<String> disabledPlugins = ConcurrentHashMap.newKeySet();
    private final CopyOnWriteArrayList<String> loadOrder = new CopyOnWriteArrayList<>();
    private volatile boolean loaded;
    private volatile boolean enabled;
    private volatile boolean closed;

    private static DefaultCustomServices defaultCustomServices(EventBus eventBus) {
        var customItems = new FandCustomItemRegistry();
        return new DefaultCustomServices(customItems, new FandCustomBlockRegistry(eventBus, customItems));
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler
    ) {
        this(pluginsDirectory, dataDirectoryRoot, parentClassLoader, commandRegistry, eventBus, permissions, scheduler,
                defaultCustomServices(eventBus), Options.defaults());
    }

    private PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            DefaultCustomServices customServices,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                new FandRecipeRegistry(),
                LootTableService.empty(),
                unavailableScoreboardService(),
                new PacketRegistryImpl(),
                null,
                RegionService.empty(),
                DataPackService.empty(),
                ResourcePackService.empty(),
                AdvancementRegistry.empty(),
                EnchantmentRegistry.empty(),
                StructureService.empty(),
                MapService.empty(),
                BossBarService.empty(),
                HologramService.empty(),
                TabListService.empty(),
                PlaceholderService.empty(),
                SimulatedPlayerService.empty(),
                customServices.items(),
                customServices.blocks(),
                new FandGuiService(eventBus),
                true,
                options
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            LootTableService lootTableService,
            ScoreboardService scoreboardService,
            PacketRegistry packetRegistry,
            PluginMessaging pluginMessaging,
            RegionService regionService,
            DataPackService dataPackService,
            ResourcePackService resourcePackService,
            AdvancementRegistry advancementRegistry,
            EnchantmentRegistry enchantmentRegistry,
            StructureService structureService,
            MapService mapService,
            BossBarService bossBarService,
            HologramService hologramService,
            TabListService tabListService,
            PlaceholderService placeholderService,
            SimulatedPlayerService simulatedPlayerService,
            CustomItemRegistry customItemRegistry,
            CustomBlockRegistry customBlockRegistry,
            GuiService guiService,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                recipeRegistry,
                lootTableService,
                scoreboardService,
                packetRegistry,
                pluginMessaging,
                regionService,
                dataPackService,
                resourcePackService,
                advancementRegistry,
                enchantmentRegistry,
                structureService,
                mapService,
                bossBarService,
                hologramService,
                tabListService,
                placeholderService,
                simulatedPlayerService,
                customItemRegistry,
                customBlockRegistry,
                guiService,
                false,
                options
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            LootTableService lootTableService,
            ScoreboardService scoreboardService,
            PacketRegistry packetRegistry,
            PluginMessaging pluginMessaging,
            RegionService regionService,
            DataPackService dataPackService,
            AdvancementRegistry advancementRegistry,
            EnchantmentRegistry enchantmentRegistry,
            StructureService structureService,
            MapService mapService,
            BossBarService bossBarService,
            HologramService hologramService,
            TabListService tabListService,
            PlaceholderService placeholderService,
            SimulatedPlayerService simulatedPlayerService,
            CustomItemRegistry customItemRegistry,
            CustomBlockRegistry customBlockRegistry,
            GuiService guiService,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                recipeRegistry,
                lootTableService,
                scoreboardService,
                packetRegistry,
                pluginMessaging,
                regionService,
                dataPackService,
                ResourcePackService.empty(),
                advancementRegistry,
                enchantmentRegistry,
                structureService,
                mapService,
                bossBarService,
                hologramService,
                tabListService,
                placeholderService,
                simulatedPlayerService,
                customItemRegistry,
                customBlockRegistry,
                guiService,
                false,
                options
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            Options options
    ) {
        this(pluginsDirectory, dataDirectoryRoot, parentClassLoader, commandRegistry, eventBus, permissions, scheduler,
                defaultCustomServices(eventBus), options);
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            Options options
    ) {
        this(pluginsDirectory, dataDirectoryRoot, parentClassLoader, commandRegistry, eventBus, permissions, scheduler,
                recipeRegistry, defaultCustomServices(eventBus), options);
    }

    private PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            DefaultCustomServices customServices,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                recipeRegistry,
                LootTableService.empty(),
                unavailableScoreboardService(),
                new PacketRegistryImpl(),
                null,
                RegionService.empty(),
                DataPackService.empty(),
                ResourcePackService.empty(),
                AdvancementRegistry.empty(),
                EnchantmentRegistry.empty(),
                StructureService.empty(),
                MapService.empty(),
                BossBarService.empty(),
                HologramService.empty(),
                TabListService.empty(),
                PlaceholderService.empty(),
                SimulatedPlayerService.empty(),
                customServices.items(),
                customServices.blocks(),
                new FandGuiService(eventBus),
                true,
                options
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            ScoreboardService scoreboardService,
            PacketRegistry packetRegistry,
            AdvancementRegistry advancementRegistry,
            EnchantmentRegistry enchantmentRegistry,
            StructureService structureService,
            MapService mapService,
            CustomItemRegistry customItemRegistry,
            CustomBlockRegistry customBlockRegistry,
            GuiService guiService,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                recipeRegistry,
                LootTableService.empty(),
                scoreboardService,
                packetRegistry,
                defaultPluginMessaging(packetRegistry),
                RegionService.empty(),
                DataPackService.empty(),
                ResourcePackService.empty(),
                advancementRegistry,
                enchantmentRegistry,
                structureService,
                mapService,
                BossBarService.empty(),
                HologramService.empty(),
                TabListService.empty(),
                PlaceholderService.empty(),
                SimulatedPlayerService.empty(),
                customItemRegistry,
                customBlockRegistry,
                guiService,
                options
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            LootTableService lootTableService,
            ScoreboardService scoreboardService,
            PacketRegistry packetRegistry,
            PluginMessaging pluginMessaging,
            AdvancementRegistry advancementRegistry,
            EnchantmentRegistry enchantmentRegistry,
            StructureService structureService,
            MapService mapService,
            CustomItemRegistry customItemRegistry,
            CustomBlockRegistry customBlockRegistry,
            GuiService guiService,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                recipeRegistry,
                lootTableService,
                scoreboardService,
                packetRegistry,
                pluginMessaging,
                RegionService.empty(),
                DataPackService.empty(),
                ResourcePackService.empty(),
                advancementRegistry,
                enchantmentRegistry,
                structureService,
                mapService,
                BossBarService.empty(),
                HologramService.empty(),
                TabListService.empty(),
                PlaceholderService.empty(),
                SimulatedPlayerService.empty(),
                customItemRegistry,
                customBlockRegistry,
                guiService,
                options
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            LootTableService lootTableService,
            ScoreboardService scoreboardService,
            PacketRegistry packetRegistry,
            PluginMessaging pluginMessaging,
            RegionService regionService,
            DataPackService dataPackService,
            ResourcePackService resourcePackService,
            AdvancementRegistry advancementRegistry,
            EnchantmentRegistry enchantmentRegistry,
            StructureService structureService,
            MapService mapService,
            BossBarService bossBarService,
            TabListService tabListService,
            PlaceholderService placeholderService,
            SimulatedPlayerService simulatedPlayerService,
            CustomItemRegistry customItemRegistry,
            CustomBlockRegistry customBlockRegistry,
            GuiService guiService,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                recipeRegistry,
                lootTableService,
                scoreboardService,
                packetRegistry,
                pluginMessaging,
                regionService,
                dataPackService,
                resourcePackService,
                advancementRegistry,
                enchantmentRegistry,
                structureService,
                mapService,
                bossBarService,
                HologramService.empty(),
                tabListService,
                placeholderService,
                simulatedPlayerService,
                customItemRegistry,
                customBlockRegistry,
                guiService,
                false,
                options
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            LootTableService lootTableService,
            ScoreboardService scoreboardService,
            PacketRegistry packetRegistry,
            PluginMessaging pluginMessaging,
            AdvancementRegistry advancementRegistry,
            EnchantmentRegistry enchantmentRegistry,
            StructureService structureService,
            MapService mapService,
            BossBarService bossBarService,
            HologramService hologramService,
            TabListService tabListService,
            CustomItemRegistry customItemRegistry,
            CustomBlockRegistry customBlockRegistry,
            GuiService guiService,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                recipeRegistry,
                lootTableService,
                scoreboardService,
                packetRegistry,
                pluginMessaging,
                RegionService.empty(),
                DataPackService.empty(),
                ResourcePackService.empty(),
                advancementRegistry,
                enchantmentRegistry,
                structureService,
                mapService,
                bossBarService,
                hologramService,
                tabListService,
                PlaceholderService.empty(),
                SimulatedPlayerService.empty(),
                customItemRegistry,
                customBlockRegistry,
                guiService,
                options
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            ScoreboardService scoreboardService,
            PacketRegistry packetRegistry,
            PluginMessaging pluginMessaging,
            AdvancementRegistry advancementRegistry,
            EnchantmentRegistry enchantmentRegistry,
            StructureService structureService,
            MapService mapService,
            CustomItemRegistry customItemRegistry,
            CustomBlockRegistry customBlockRegistry,
            GuiService guiService,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                recipeRegistry,
                LootTableService.empty(),
                scoreboardService,
                packetRegistry,
                pluginMessaging,
                RegionService.empty(),
                DataPackService.empty(),
                ResourcePackService.empty(),
                advancementRegistry,
                enchantmentRegistry,
                structureService,
                mapService,
                BossBarService.empty(),
                HologramService.empty(),
                TabListService.empty(),
                PlaceholderService.empty(),
                SimulatedPlayerService.empty(),
                customItemRegistry,
                customBlockRegistry,
                guiService,
                false,
                options
        );
    }

    private PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            LootTableService lootTableService,
            ScoreboardService scoreboardService,
            PacketRegistry packetRegistry,
            PluginMessaging pluginMessaging,
            RegionService regionService,
            DataPackService dataPackService,
            ResourcePackService resourcePackService,
            AdvancementRegistry advancementRegistry,
            EnchantmentRegistry enchantmentRegistry,
            StructureService structureService,
            MapService mapService,
            BossBarService bossBarService,
            HologramService hologramService,
            TabListService tabListService,
            PlaceholderService placeholderService,
            SimulatedPlayerService simulatedPlayerService,
            CustomItemRegistry customItemRegistry,
            CustomBlockRegistry customBlockRegistry,
            GuiService guiService,
            boolean closeGuiService,
            Options options
    ) {
        this.pluginsDirectory = pluginsDirectory;
        this.dataDirectoryRoot = dataDirectoryRoot;
        this.libraryResolver = new PluginLibraryResolver(
                pluginsDirectory.toAbsolutePath().normalize().resolveSibling("libraries")
        );
        this.parentClassLoader = parentClassLoader;
        this.commandRegistry = commandRegistry;
        this.eventBus = eventBus;
        this.permissions = permissions;
        this.recipeRegistry = recipeRegistry;
        this.lootTableService = lootTableService;
        this.advancementRegistry = advancementRegistry;
        this.enchantmentRegistry = enchantmentRegistry;
        this.structureService = structureService;
        this.mapService = mapService;
        this.bossBarService = bossBarService;
        this.hologramService = hologramService;
        this.tabListService = tabListService;
        this.simulatedPlayerService = simulatedPlayerService;
        this.placeholderService = placeholderService;
        this.scoreboardService = scoreboardService;
        this.packetRegistry = packetRegistry;
        this.pluginMessaging = pluginMessaging == null ? defaultPluginMessaging(packetRegistry) : pluginMessaging;
        this.regionService = Objects.requireNonNull(regionService, "regionService");
        this.dataPackService = Objects.requireNonNull(dataPackService, "dataPackService");
        this.resourcePackService = Objects.requireNonNull(resourcePackService, "resourcePackService");
        this.customItemRegistry = customItemRegistry;
        this.customBlockRegistry = customBlockRegistry;
        this.guiService = guiService;
        this.closeGuiService = closeGuiService;
        this.scheduler = scheduler;
        this.options = options;
    }

    public void reconfigure(Options options) {
        this.options = options;
    }

    public void gameRuleService(GameRuleService service) {
        this.gameRuleService = Objects.requireNonNull(service, "service");
    }

    public void integrations(ExternalIntegrationStrategy strategy) {
        this.integrationStrategy = Objects.requireNonNull(strategy, "strategy");
    }

    public void serviceRegistry(ServiceRegistry registry) {
        this.serviceRegistry = Objects.requireNonNull(registry, "registry");
    }

    public void nmsService(NmsService service) {
        this.nmsService = Objects.requireNonNull(service, "service");
    }

    public void loginAuthenticationService(LoginAuthenticationService service) {
        this.loginAuthenticationService = Objects.requireNonNull(service, "service");
    }

    @Override
    public ServiceRegistry services() {
        return serviceRegistry;
    }

    public void loadPlugins() {
        synchronized (lifecycleLock) {
            ensureOpen();
            if (loaded) {
                throw new IllegalStateException("Plugins are already loaded");
            }
            try {
                Files.createDirectories(pluginsDirectory);
                Files.createDirectories(dataDirectoryRoot);
                var discovered = discoverArtifacts();
                var sorted = sortArtifacts(discovered.artifacts());
                var ordered = sorted.artifacts();
                var skipped = discovered.skipped() + sorted.skipped();
                for (var artifact : ordered) {
                    var unavailableDependency = firstUnavailableDependency(artifact.descriptor.depends());
                    if (unavailableDependency != null) {
                        if (!options.continueOnLoadFailure()) {
                            close();
                            throw new PluginLoadException("Plugin '" + artifact.descriptor.id() + "' depends on unavailable plugin '" + unavailableDependency + "'");
                        }
                        skipped++;
                        recordDependencyError(artifact, unavailableDependency, "load");
                        LOGGER.warn("Skipping plugin {} because dependency {} is unavailable", artifact.descriptor.id(), unavailableDependency);
                        continue;
                    }
                    try {
                        loadArtifact(artifact);
                    } catch (Throwable failure) {
                        if (!options.continueOnLoadFailure()) {
                            close();
                            throw new PluginLoadException("Failed to load plugin '" + artifact.descriptor.id() + "'", failure);
                        }
                        skipped++;
                        recordError(artifact.descriptor.id(), artifact.descriptor, artifact.jarPath, PluginLifecycle.ERROR, "load", failure);
                        LOGGER.warn("Skipping plugin {} after load failure", artifact.descriptor.id(), failure);
                    }
                }
                loaded = true;
                if (options.logSummary()) {
                    LOGGER.info(
                            "Plugin load summary: discovered={}, loaded={}, skipped={}",
                            discovered.artifacts().size() + discovered.skipped(),
                            loadedPlugins.size(),
                            skipped
                    );
                }
            } catch (IOException failure) {
                close();
                throw new PluginLoadException("Failed to scan plugins directory", failure);
            } catch (RuntimeException failure) {
                close();
                throw failure;
            } catch (Error failure) {
                close();
                throw failure;
            }
        }
    }

    public void enablePlugins() {
        synchronized (lifecycleLock) {
            ensureOpen();
            if (!loaded) {
                throw new IllegalStateException("Plugins must be loaded before enabling");
            }
            if (enabled) {
                throw new IllegalStateException("Plugins are already enabled");
            }
            var enabledThisRun = new ArrayDeque<LoadedPlugin>();
            var skipped = 0;
            try {
                for (var pluginId : loadOrder) {
                    var loadedPlugin = loadedPlugins.get(pluginId);
                    if (loadedPlugin == null || loadedPlugin.enabled || disabledPlugins.contains(pluginId)) {
                        continue;
                    }
                    var unavailableDependency = firstDisabledDependency(loadedPlugin.descriptor.depends());
                    if (unavailableDependency != null) {
                        if (!options.continueOnEnableFailure()) {
                            throw new PluginLoadException("Plugin '" + loadedPlugin.descriptor.id() + "' depends on disabled plugin '" + unavailableDependency + "'");
                        }
                        skipped++;
                        recordDependencyError(new PluginArtifact(loadedPlugin.jarPath, loadedPlugin.descriptor), unavailableDependency, "enable");
                        LOGGER.warn("Skipping plugin {} because dependency {} is disabled", loadedPlugin.descriptor.id(), unavailableDependency);
                        discardLoadedPlugin(loadedPlugin);
                        continue;
                    }
                    try {
                        enableLoadedPlugin(loadedPlugin);
                        enabledThisRun.push(loadedPlugin);
                    } catch (Throwable failure) {
                        discardLoadedPlugin(loadedPlugin);
                        if (!options.continueOnEnableFailure()) {
                            throw new PluginLoadException("Failed to enable plugin '" + loadedPlugin.descriptor.id() + "'", failure);
                        }
                        skipped++;
                        recordError(loadedPlugin.descriptor.id(), loadedPlugin.descriptor, loadedPlugin.jarPath, PluginLifecycle.ERROR, "enable", failure);
                        LOGGER.warn("Skipping plugin {} after enable failure", loadedPlugin.descriptor.id(), failure);
                    }
                }
                enabled = true;
                if (options.logSummary()) {
                    LOGGER.info("Plugin enable summary: loaded={}, enabled={}, skipped={}", loadedPlugins.size(), enabledThisRun.size(), skipped);
                }
            } catch (Throwable failure) {
                while (!enabledThisRun.isEmpty()) {
                    disablePlugin(enabledThisRun.pop());
                }
                throw failure;
            }
        }
    }

    public void disablePlugins() {
        if (!enabled) {
            return;
        }
        for (int i = loadOrder.size() - 1; i >= 0; i--) {
            var loadedPlugin = loadedPlugins.get(loadOrder.get(i));
            if (loadedPlugin != null) {
                disablePlugin(loadedPlugin);
            }
        }
        enabled = false;
    }

    @Override
    public Collection<Plugin> loaded() {
        var plugins = new ArrayList<Plugin>(loadOrder.size());
        for (var pluginId : loadOrder) {
            var loadedPlugin = loadedPlugins.get(pluginId);
            if (loadedPlugin != null) {
                plugins.add(loadedPlugin.plugin);
            }
        }
        return List.copyOf(plugins);
    }

    @Override
    public Collection<PluginDescriptor> loadedDescriptors() {
        var descriptors = new ArrayList<PluginDescriptor>(loadOrder.size());
        for (var pluginId : loadOrder) {
            var loadedPlugin = loadedPlugins.get(pluginId);
            if (loadedPlugin != null) {
                descriptors.add(loadedPlugin.descriptor);
            }
        }
        return List.copyOf(descriptors);
    }

    @Override
    public Optional<Plugin> byId(String id) {
        var loadedPlugin = loadedPlugins.get(id);
        return loadedPlugin == null ? Optional.empty() : Optional.of(loadedPlugin.plugin);
    }

    @Override
    public boolean isEnabled(String id) {
        var loadedPlugin = loadedPlugins.get(id);
        return loadedPlugin != null && loadedPlugin.enabled;
    }

    public Path pluginsDirectory() {
        return pluginsDirectory;
    }

    public List<PluginStatus> pluginStatuses(boolean includeAvailable) {
        if (includeAvailable) {
            refreshAvailableStatuses();
        }
        var dependents = dependentsById();
        var orderedIds = new LinkedHashSet<String>();
        orderedIds.addAll(loadOrder);
        statusEntries.keySet().stream().sorted().forEach(orderedIds::add);
        var statuses = new ArrayList<PluginStatus>();
        for (var id : orderedIds) {
            var status = pluginStatus(id, dependents.getOrDefault(id, List.of()));
            if (status.isPresent() && (includeAvailable || status.get().lifecycle() != PluginLifecycle.AVAILABLE)) {
                statuses.add(status.get());
            }
        }
        return List.copyOf(statuses);
    }

    public Optional<PluginStatus> pluginStatus(String id) {
        refreshAvailableStatuses();
        return pluginStatus(id, dependentsById().getOrDefault(id, List.of()));
    }

    public List<String> pluginIdSuggestions() {
        refreshAvailableStatuses();
        var ids = new LinkedHashSet<String>();
        ids.addAll(loadOrder);
        statusEntries.keySet().stream().sorted().forEach(ids::add);
        return List.copyOf(ids);
    }

    public List<String> loadSuggestions() {
        var suggestions = new LinkedHashSet<String>();
        try {
            for (var artifact : scanArtifacts()) {
                if (!loadedPlugins.containsKey(artifact.descriptor.id())) {
                    suggestions.add(artifact.descriptor.id());
                    suggestions.add(artifact.jarPath.getFileName().toString());
                }
            }
        } catch (IOException failure) {
            LOGGER.warn("Failed to scan plugin load suggestions", failure);
        }
        return List.copyOf(suggestions);
    }

    public PluginOperationResult loadPlugin(String target) {
        synchronized (lifecycleLock) {
            ensureOpen();
            try {
                Files.createDirectories(pluginsDirectory);
                Files.createDirectories(dataDirectoryRoot);
                var artifact = resolveArtifact(target);
                var id = artifact.descriptor.id();
                if (loadedPlugins.containsKey(id)) {
                    return PluginOperationResult.failure("Plugin '" + id + "' is already loaded");
                }
                var unavailableDependency = firstUnavailableDependency(artifact.descriptor.depends());
                if (unavailableDependency != null) {
                    var message = "Plugin '" + id + "' depends on unavailable plugin '" + unavailableDependency + "'";
                    recordDependencyError(artifact, unavailableDependency, "load");
                    return PluginOperationResult.failure(message);
                }
                if (enabled) {
                    var disabledDependency = firstDisabledDependency(artifact.descriptor.depends());
                    if (disabledDependency != null) {
                        var message = "Plugin '" + id + "' depends on disabled plugin '" + disabledDependency + "'";
                        recordDependencyError(artifact, disabledDependency, "enable");
                        return PluginOperationResult.failure(message);
                    }
                }
                var loadedPlugin = loadArtifact(artifact);
                disabledPlugins.remove(id);
                if (enabled) {
                    enableLoadedPlugin(loadedPlugin);
                }
                return PluginOperationResult.success("Loaded plugin '" + id + "'");
            } catch (Throwable failure) {
                return failureResult("load", target, failure);
            }
        }
    }

    public PluginOperationResult unloadPlugin(String id, boolean cascade) {
        synchronized (lifecycleLock) {
            ensureOpen();
            var normalized = normalizePluginId(id);
            var loadedPlugin = loadedPlugins.get(normalized);
            if (loadedPlugin == null) {
                return PluginOperationResult.failure("Plugin '" + normalized + "' is not loaded");
            }
            var dependents = loadedDependentsRecursive(normalized);
            if (!dependents.isEmpty() && !cascade) {
                return PluginOperationResult.failure("Plugin '" + normalized + "' is required by " + String.join(", ", dependents));
            }
            for (var dependent : reverseLoadOrder(dependents)) {
                var dependentPlugin = loadedPlugins.get(dependent);
                if (dependentPlugin != null) {
                    unloadLoadedPlugin(dependentPlugin, false, "unload");
                }
            }
            unloadLoadedPlugin(loadedPlugin, false, "unload");
            return PluginOperationResult.success("Unloaded plugin '" + normalized + "'");
        }
    }

    public PluginOperationResult disablePlugin(String id, boolean cascade) {
        synchronized (lifecycleLock) {
            ensureOpen();
            var normalized = normalizePluginId(id);
            var loadedPlugin = loadedPlugins.get(normalized);
            if (loadedPlugin == null) {
                disabledPlugins.add(normalized);
                var entry = statusEntries.get(normalized);
                if (entry != null) {
                    entry.lifecycle = PluginLifecycle.DISABLED;
                    entry.disabledAtMillis = System.currentTimeMillis();
                }
                return PluginOperationResult.success("Disabled plugin '" + normalized + "'");
            }
            var dependents = loadedDependentsRecursive(normalized);
            if (!dependents.isEmpty() && !cascade) {
                return PluginOperationResult.failure("Plugin '" + normalized + "' is required by " + String.join(", ", dependents));
            }
            for (var dependent : reverseLoadOrder(dependents)) {
                var dependentPlugin = loadedPlugins.get(dependent);
                if (dependentPlugin != null) {
                    disabledPlugins.add(dependent);
                    unloadLoadedPlugin(dependentPlugin, true, "disable");
                }
            }
            disabledPlugins.add(normalized);
            unloadLoadedPlugin(loadedPlugin, true, "disable");
            return PluginOperationResult.success("Disabled plugin '" + normalized + "'");
        }
    }

    public PluginOperationResult enablePlugin(String id) {
        synchronized (lifecycleLock) {
            ensureOpen();
            var normalized = normalizePluginId(id);
            disabledPlugins.remove(normalized);
            var loadedPlugin = loadedPlugins.get(normalized);
            if (loadedPlugin != null) {
                if (loadedPlugin.enabled) {
                    return PluginOperationResult.success("Plugin '" + normalized + "' is already enabled");
                }
                try {
                    enableLoadedPlugin(loadedPlugin);
                    return PluginOperationResult.success("Enabled plugin '" + normalized + "'");
                } catch (Throwable failure) {
                    return failureResult("enable", normalized, failure);
                }
            }
            return loadPlugin(normalized);
        }
    }

    public PluginOperationResult reloadPlugin(String target, boolean cascade) {
        synchronized (lifecycleLock) {
            ensureOpen();
            if ("all".equalsIgnoreCase(target)) {
                return reloadAllPlugins();
            }
            var normalized = normalizePluginId(target);
            if (loadedPlugins.containsKey(normalized)) {
                var unload = unloadPlugin(normalized, cascade);
                if (!unload.success()) {
                    return unload;
                }
            }
            return loadPlugin(target);
        }
    }

    public PluginOperationResult reloadAllPlugins() {
        synchronized (lifecycleLock) {
            ensureOpen();
            var disabledSnapshot = Set.copyOf(disabledPlugins);
            for (int i = loadOrder.size() - 1; i >= 0; i--) {
                var loadedPlugin = loadedPlugins.get(loadOrder.get(i));
                if (loadedPlugin != null) {
                    unloadLoadedPlugin(loadedPlugin, disabledSnapshot.contains(loadedPlugin.descriptor.id()), "reload");
                }
            }
            loadedPlugins.clear();
            loadOrder.clear();
            loaded = false;
            enabled = false;
            try {
                loadPlugins();
                enablePlugins();
                for (var disabledId : disabledSnapshot) {
                    var loadedPlugin = loadedPlugins.get(disabledId);
                    if (loadedPlugin != null) {
                        disabledPlugins.add(disabledId);
                        unloadLoadedPlugin(loadedPlugin, true, "disable");
                    }
                }
                return PluginOperationResult.success("Reloaded all plugins");
            } catch (Throwable failure) {
                return failureResult("reload", "all", failure);
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        disablePlugins();
        for (int i = loadOrder.size() - 1; i >= 0; i--) {
            var pluginId = loadOrder.get(i);
            var loadedPlugin = loadedPlugins.get(pluginId);
            if (loadedPlugin == null) {
                continue;
            }
            closeQuietly(loadedPlugin.context, loadedPlugin.descriptor.id());
            closeQuietly(loadedPlugin.classLoader, loadedPlugin.descriptor.id());
        }
        loadedPlugins.clear();
        loadOrder.clear();
        statusEntries.clear();
        disabledPlugins.clear();
        loaded = false;
        enabled = false;
        try {
            libraryResolver.close();
        } catch (RuntimeException failure) {
            LOGGER.warn("Failed to close plugin library resolver", failure);
        }
        if (closeGuiService && guiService instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception failure) {
                LOGGER.warn("Failed to close plugin runtime GUI service", failure);
            }
        }
    }

    private LoadedPlugin loadArtifact(PluginArtifact artifact) {
        var id = artifact.descriptor.id();
        if (loadedPlugins.containsKey(id)) {
            throw new PluginLoadException("Plugin '" + id + "' is already loaded");
        }
        var dependencies = dependencyClassLoaders(
                artifact.descriptor.depends(),
                artifact.descriptor.loadAfter());
        var libraryUrls = libraryResolver.resolve(artifact.jarPath, id).stream()
                .map(PluginRuntime::toJarUrl)
                .toList();
        var classLoader = new PluginClassLoader(toJarUrl(artifact.jarPath), libraryUrls, parentClassLoader, dependencies);
        var resources = new PluginResourceTracker();
        var pluginPlaceholders = new PluginPlaceholderService(placeholderService, resources, id);
        var context = new RuntimePluginContext(
                artifact.descriptor,
                LoggerFactory.getLogger(id),
                new PluginEventBus(eventBus, resources, id),
                permissions,
                new PluginCommandRegistry(commandRegistry, resources, id),
                new PluginRecipeRegistry(recipeRegistry, resources, id),
                new PluginLootTableService(lootTableService, resources, artifact.descriptor.id()),
                new PluginAdvancementRegistry(advancementRegistry, resources, id),
                new PluginEnchantmentRegistry(enchantmentRegistry, resources, id),
                new PluginStructureService(structureService, resources, artifact.descriptor.id()),
                new PluginMapService(mapService, resources),
                new PluginBossBarService(bossBarService, resources, id),
                new PluginHologramService(hologramService, resources),
                new PluginTabListService(tabListService, resources, tabListVisibilityRegistry),
                new PluginSimulatedPlayerService(simulatedPlayerService, resources),
                pluginPlaceholders,
                new FandMiniMessageService(pluginPlaceholders),
                new PluginScoreboardService(scoreboardService, resources, id),
                new PluginPacketRegistry(packetRegistry, resources),
                new PluginPluginMessaging(pluginMessaging, resources),
                new PluginGameRuleService(gameRuleService, resources, id),
                new PluginRegionService(regionService, resources, id),
                new PluginDataPackService(dataPackService, resources, id),
                new PluginResourcePackService(resourcePackService, resources, id),
                integrationStrategy,
                new PluginServiceRegistry(serviceRegistry, resources, id),
                new PluginNmsService(nmsService, resources, id),
                new PluginLoginAuthenticationService(loginAuthenticationService, resources, id),
                new PluginCustomItemRegistry(customItemRegistry, resources, id),
                new PluginCustomBlockRegistry(customBlockRegistry, resources, id),
                new PluginGuiService(guiService, resources),
                new PluginScheduler(scheduler, resources),
                dataDirectoryRoot.resolve(id),
                resources,
                classLoader
        );
        try {
            registerDeclaredPermissions(artifact.descriptor, resources);
            var plugin = instantiatePlugin(artifact.descriptor, classLoader);
            plugin.onLoad(context);
            var loadedPlugin = new LoadedPlugin(artifact.descriptor, artifact.jarPath, plugin, context, classLoader, resources);
            loadedPlugins.put(id, loadedPlugin);
            loadOrder.addIfAbsent(id);
            recordState(id, artifact.descriptor, artifact.jarPath, PluginLifecycle.DISABLED, null);
            var entry = statusEntries.get(id);
            if (entry != null) {
                entry.loadedAtMillis = System.currentTimeMillis();
            }
            return loadedPlugin;
        } catch (Throwable failure) {
            closeQuietly(context, id);
            closeQuietly(classLoader, id);
            throw failure;
        }
    }

    private void enableLoadedPlugin(LoadedPlugin loadedPlugin) {
        if (loadedPlugin.enabled) {
            return;
        }
        loadedPlugin.plugin.onEnable(loadedPlugin.context);
        loadedPlugin.enabled = true;
        var now = System.currentTimeMillis();
        var entry = recordState(loadedPlugin.descriptor.id(), loadedPlugin.descriptor, loadedPlugin.jarPath, PluginLifecycle.ENABLED, null);
        entry.enabledAtMillis = now;
        disabledPlugins.remove(loadedPlugin.descriptor.id());
        firePluginEnableEvent(loadedPlugin.descriptor);
    }

    private void unloadLoadedPlugin(LoadedPlugin loadedPlugin, boolean keepDisabled, String phase) {
        var id = loadedPlugin.descriptor.id();
        boolean wasEnabled = loadedPlugin.enabled;
        Throwable failure = null;
        try {
            if (wasEnabled) {
                loadedPlugin.plugin.onDisable(loadedPlugin.context);
            }
        } catch (Throwable ex) {
            failure = ex;
            LOGGER.warn("Plugin {} failed during {}", id, phase, ex);
        } finally {
            if (wasEnabled) {
                firePluginDisableEvent(loadedPlugin.descriptor);
            }
            loadedPlugin.enabled = false;
            loadedPlugins.remove(id, loadedPlugin);
            loadOrder.remove(id);
            if (keepDisabled) {
                disabledPlugins.add(id);
            } else {
                disabledPlugins.remove(id);
            }
            closeQuietly(loadedPlugin.context, id);
            if (permissions instanceof io.fand.server.permission.PermissionManager manager) {
                manager.unregisterNamespaces(new HashSet<>(permissionNamespaces(id)));
            }
            closeQuietly(loadedPlugin.classLoader, id);
            var lifecycle = keepDisabled ? PluginLifecycle.DISABLED : PluginLifecycle.AVAILABLE;
            var entry = failure == null
                    ? recordState(id, loadedPlugin.descriptor, loadedPlugin.jarPath, lifecycle, null)
                    : recordError(id, loadedPlugin.descriptor, loadedPlugin.jarPath, PluginLifecycle.ERROR, phase, failure);
            entry.disabledAtMillis = System.currentTimeMillis();
        }
    }

    private PluginArtifact resolveArtifact(String target) throws IOException {
        var trimmed = Objects.requireNonNull(target, "target").trim();
        if (trimmed.isEmpty()) {
            throw new PluginLoadException("Plugin target is required");
        }
        if (trimmed.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            var jarPath = pluginsDirectory.resolve(trimmed).normalize();
            var root = pluginsDirectory.toAbsolutePath().normalize();
            if (!jarPath.toAbsolutePath().normalize().startsWith(root)) {
                throw new PluginLoadException("Plugin jar must be inside the plugins directory");
            }
            if (!Files.isRegularFile(jarPath)) {
                throw new PluginLoadException("Plugin jar not found: " + trimmed);
            }
            var descriptor = readDescriptor(jarPath);
            return new PluginArtifact(jarPath, descriptor);
        }
        var id = normalizePluginId(trimmed);
        PluginArtifact match = null;
        for (var artifact : scanArtifacts()) {
            if (!artifact.descriptor.id().equals(id)) {
                continue;
            }
            if (match != null) {
                throw new PluginLoadException("Duplicate plugin id '" + id + "' in " + match.jarPath + " and " + artifact.jarPath);
            }
            match = artifact;
        }
        if (match == null) {
            throw new PluginLoadException("Plugin '" + id + "' was not found in " + pluginsDirectory);
        }
        return match;
    }

    private List<PluginArtifact> scanArtifacts() throws IOException {
        Files.createDirectories(pluginsDirectory);
        try (var stream = Files.list(pluginsDirectory)) {
            var jars = stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
            var artifacts = new ArrayList<PluginArtifact>(jars.size());
            for (var jar : jars) {
                try {
                    artifacts.add(new PluginArtifact(jar, readDescriptor(jar)));
                } catch (PluginLoadException failure) {
                    recordError(fileStatusId(jar), null, jar, PluginLifecycle.ERROR, "descriptor", failure);
                }
            }
            return artifacts;
        }
    }

    private void refreshAvailableStatuses() {
        try {
            for (var artifact : scanArtifacts()) {
                if (!loadedPlugins.containsKey(artifact.descriptor.id()) && !disabledPlugins.contains(artifact.descriptor.id())) {
                    recordState(artifact.descriptor.id(), artifact.descriptor, artifact.jarPath, PluginLifecycle.AVAILABLE, null);
                }
            }
        } catch (IOException failure) {
            LOGGER.warn("Failed to refresh plugin status", failure);
        }
    }

    private Optional<PluginStatus> pluginStatus(String id, List<String> dependents) {
        var normalized = normalizePluginId(id);
        var loadedPlugin = loadedPlugins.get(normalized);
        if (loadedPlugin != null) {
            var lifecycle = loadedPlugin.enabled ? PluginLifecycle.ENABLED : PluginLifecycle.DISABLED;
            var entry = recordState(normalized, loadedPlugin.descriptor, loadedPlugin.jarPath, lifecycle, null);
            return Optional.of(toStatus(entry, loadedPlugin, dependents));
        }
        var entry = statusEntries.get(normalized);
        return entry == null ? Optional.empty() : Optional.of(toStatus(entry, null, dependents));
    }

    private PluginStatus toStatus(StatusEntry entry, LoadedPlugin loadedPlugin, List<String> dependents) {
        var descriptor = loadedPlugin == null ? entry.descriptor : loadedPlugin.descriptor;
        var dependencies = descriptor == null ? List.<String>of() : descriptor.depends();
        var commands = loadedPlugin == null ? List.<CommandInfo>of() : loadedPlugin.resources.commandDescriptors();
        var runtimePermissions = loadedPlugin == null ? List.<PermissionDescriptor>of() : loadedPlugin.resources.permissionDescriptors();
        var declaredPermissions = descriptor == null ? List.<PermissionDescriptor>of() : descriptor.permissions();
        var permissions = new ArrayList<PermissionDescriptor>(declaredPermissions.size() + runtimePermissions.size());
        permissions.addAll(declaredPermissions);
        permissions.addAll(runtimePermissions);
        return new PluginStatus(
                entry.id,
                entry.lifecycle,
                loadedPlugin != null && loadedPlugin.enabled,
                descriptor,
                loadedPlugin == null ? entry.jarPath : loadedPlugin.jarPath,
                dataDirectoryRoot.resolve(entry.id),
                dependencies,
                dependents,
                entry.lastError,
                entry.loadedAtMillis,
                entry.enabledAtMillis,
                entry.disabledAtMillis,
                commands,
                permissions
        );
    }

    private LinkedHashMap<String, List<String>> dependentsById() {
        var dependents = new LinkedHashMap<String, List<String>>();
        for (var entry : statusEntries.values()) {
            if (entry.descriptor == null) {
                continue;
            }
            for (var dependency : entry.descriptor.depends()) {
                dependents.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(entry.descriptor.id());
            }
        }
        for (var loadedPlugin : loadedPlugins.values()) {
            for (var dependency : loadedPlugin.descriptor.depends()) {
                dependents.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(loadedPlugin.descriptor.id());
            }
        }
        dependents.replaceAll((ignored, values) -> values.stream().distinct().sorted().toList());
        return dependents;
    }

    private List<String> loadedDependents(String pluginId) {
        var dependents = new ArrayList<String>();
        for (var loadedPlugin : loadedPlugins.values()) {
            if (loadedPlugin.descriptor.depends().contains(pluginId)) {
                dependents.add(loadedPlugin.descriptor.id());
            }
        }
        dependents.sort(Comparator.naturalOrder());
        return dependents;
    }

    private List<String> loadedDependentsRecursive(String pluginId) {
        var dependents = new LinkedHashSet<String>();
        collectLoadedDependents(pluginId, dependents);
        return List.copyOf(dependents);
    }

    private void collectLoadedDependents(String pluginId, LinkedHashSet<String> dependents) {
        for (var dependent : loadedDependents(pluginId)) {
            if (dependents.add(dependent)) {
                collectLoadedDependents(dependent, dependents);
            }
        }
    }

    private List<String> reverseLoadOrder(Collection<String> pluginIds) {
        var wanted = new HashSet<>(pluginIds);
        var ordered = new ArrayList<String>();
        for (int i = loadOrder.size() - 1; i >= 0; i--) {
            var id = loadOrder.get(i);
            if (wanted.contains(id)) {
                ordered.add(id);
            }
        }
        return ordered;
    }

    private StatusEntry recordState(
            String id,
            PluginDescriptor descriptor,
            Path jarPath,
            PluginLifecycle lifecycle,
            PluginError error
    ) {
        var normalized = normalizePluginId(id);
        var entry = statusEntries.computeIfAbsent(normalized, StatusEntry::new);
        entry.descriptor = descriptor == null ? entry.descriptor : descriptor;
        entry.jarPath = jarPath == null ? entry.jarPath : jarPath;
        entry.lifecycle = lifecycle;
        if (error != null) {
            entry.lastError = error;
        } else if (lifecycle != PluginLifecycle.ERROR) {
            entry.lastError = null;
        }
        return entry;
    }

    private StatusEntry recordError(
            String id,
            PluginDescriptor descriptor,
            Path jarPath,
            PluginLifecycle lifecycle,
            String phase,
            Throwable failure
    ) {
        return recordState(
                id,
                descriptor,
                jarPath,
                lifecycle,
                new PluginError(phase, rootMessage(failure), System.currentTimeMillis())
        );
    }

    private void recordDependencyError(PluginArtifact artifact, String dependency, String phase) {
        recordError(
                artifact.descriptor.id(),
                artifact.descriptor,
                artifact.jarPath,
                PluginLifecycle.ERROR,
                phase,
                new PluginLoadException("Missing or disabled dependency '" + dependency + "'")
        );
    }

    private PluginOperationResult failureResult(String phase, String target, Throwable failure) {
        var message = rootMessage(failure);
        try {
            var entry = statusEntries.get(normalizePluginId(target));
            if (entry != null) {
                recordError(entry.id, entry.descriptor, entry.jarPath, PluginLifecycle.ERROR, phase, failure);
            }
        } catch (IllegalArgumentException ignored) {
            // File-name targets are not necessarily plugin ids.
        }
        return PluginOperationResult.failure("Failed to " + phase + " plugin '" + target + "': " + message);
    }

    private static String rootMessage(Throwable failure) {
        var cursor = failure;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }

    private static String normalizePluginId(String id) {
        var normalized = Objects.requireNonNull(id, "id").trim().toLowerCase(Locale.ROOT);
        if (!PLUGIN_ID.matcher(normalized).matches()) {
            throw new PluginLoadException("Invalid plugin id '" + id + "'");
        }
        return normalized;
    }

    private static String fileStatusId(Path jarPath) {
        var name = jarPath.getFileName().toString().toLowerCase(Locale.ROOT);
        var base = name.endsWith(".jar") ? name.substring(0, name.length() - 4) : name;
        var normalized = base.replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
        if (normalized.isBlank() || !PLUGIN_ID.matcher(normalized).matches()) {
            return "jar-" + Integer.toUnsignedString(name.hashCode(), 16);
        }
        return normalized;
    }

    private DiscoveryResult discoverArtifacts() throws IOException {
        try (var stream = Files.list(pluginsDirectory)) {
            var jars = stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
            var artifacts = new ArrayList<PluginArtifact>(jars.size());
            var seenIds = new LinkedHashMap<String, Path>();
            var skipped = 0;
            for (var jar : jars) {
                PluginDescriptor descriptor;
                try {
                    descriptor = readDescriptor(jar);
                } catch (PluginLoadException failure) {
                    recordError(fileStatusId(jar), null, jar, PluginLifecycle.ERROR, "descriptor", failure);
                    if (!options.continueOnLoadFailure()) {
                        throw failure;
                    }
                    skipped++;
                    LOGGER.warn("Skipping plugin jar {}: {}", jar, failure.getMessage(), failure);
                    continue;
                }
                var existing = seenIds.putIfAbsent(descriptor.id(), jar);
                if (existing != null) {
                    recordError(
                            descriptor.id(),
                            descriptor,
                            jar,
                            PluginLifecycle.ERROR,
                            "descriptor",
                            new PluginLoadException("Duplicate plugin id '" + descriptor.id() + "' in " + existing + " and " + jar)
                    );
                    if (!options.continueOnLoadFailure()) {
                        throw new PluginLoadException("Duplicate plugin id '" + descriptor.id() + "' in " + existing + " and " + jar);
                    }
                    skipped++;
                    LOGGER.warn("Skipping duplicate plugin {} from {} because {} already provides it", descriptor.id(), jar, existing);
                    continue;
                }
                recordState(descriptor.id(), descriptor, jar, PluginLifecycle.AVAILABLE, null);
                artifacts.add(new PluginArtifact(jar, descriptor));
            }
            return new DiscoveryResult(artifacts, skipped);
        }
    }

    private PluginDescriptor readDescriptor(Path jarPath) {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getJarEntry(DESCRIPTOR_PATH);
            if (entry == null) {
                throw unsupportedPluginJar(jarPath, jar);
            }
            try (var reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                var file = GSON.fromJson(reader, DescriptorFile.class);
                if (file == null) {
                    throw new PluginLoadException("Plugin jar " + jarPath + " contains an empty " + DESCRIPTOR_PATH);
                }
                return validateDescriptor(jarPath, file.toDescriptor());
            }
        } catch (IOException | JsonParseException | IllegalArgumentException failure) {
            throw new PluginLoadException("Failed to read descriptor from " + jarPath, failure);
        }
    }

    private static PluginLoadException unsupportedPluginJar(Path jarPath, JarFile jar) {
        for (var descriptor : FOREIGN_PLUGIN_DESCRIPTORS.entrySet()) {
            if (jar.getJarEntry(descriptor.getKey()) != null) {
                return new PluginLoadException("Plugin jar " + jarPath + " contains " + descriptor.getKey()
                        + " and appears to be a " + descriptor.getValue()
                        + " plugin, not a Fand plugin. Fand plugins must declare " + DESCRIPTOR_PATH + ".");
            }
        }
        return new PluginLoadException("Plugin jar " + jarPath + " is missing " + DESCRIPTOR_PATH);
    }

    private PluginDescriptor validateDescriptor(Path jarPath, PluginDescriptor descriptor) {
        if (!PLUGIN_ID.matcher(descriptor.id()).matches()) {
            throw new PluginLoadException("Plugin jar " + jarPath + " has invalid id '" + descriptor.id() + "'");
        }
        if (!descriptor.id().equals(descriptor.id().toLowerCase(Locale.ROOT))) {
            throw new PluginLoadException("Plugin jar " + jarPath + " must use a lowercase id");
        }
        if (descriptor.version().isBlank()) {
            throw new PluginLoadException("Plugin jar " + jarPath + " must declare a non-empty version");
        }
        if (descriptor.mainClass().isBlank()) {
            throw new PluginLoadException("Plugin jar " + jarPath + " must declare a non-empty mainClass");
        }
        if (descriptor.apiVersion().isBlank()) {
            throw new PluginLoadException("Plugin jar " + jarPath + " must declare a non-empty apiVersion");
        }
        validateWebsite(jarPath, descriptor.website());
        validatePluginIds(jarPath, descriptor.depends(), "dependency");
        validatePluginIds(jarPath, descriptor.loadAfter(), "loadAfter");
        validatePluginIds(jarPath, descriptor.loadBefore(), "loadBefore");
        for (var permission : descriptor.permissions()) {
            validateDescriptorPermission(jarPath, descriptor.id(), permission);
        }
        return descriptor;
    }

    private static void validateWebsite(Path jarPath, String website) {
        if (website.isBlank()) {
            return;
        }
        try {
            var uri = URI.create(website);
            var scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("unsupported scheme");
            }
        } catch (IllegalArgumentException failure) {
            throw new PluginLoadException("Plugin jar " + jarPath + " has invalid website '" + website + "'", failure);
        }
    }

    private static void validatePluginIds(Path jarPath, List<String> ids, String field) {
        for (var id : ids) {
            if (!PLUGIN_ID.matcher(id).matches()) {
                throw new PluginLoadException("Plugin jar " + jarPath + " has invalid " + field + " id '" + id + "'");
            }
        }
    }

    private void registerDeclaredPermissions(PluginDescriptor descriptor, PluginResourceTracker resources) {
        for (var permission : descriptor.permissions()) {
            permissions.register(permission);
            resources.trackPermission(permission);
        }
    }

    private static void validateDescriptorPermission(Path jarPath, String pluginId, PermissionDescriptor permission) {
        try {
            validatePluginPermissionNode(pluginId, permission.node());
            for (var child : permission.children().keySet()) {
                validatePluginPermissionNode(pluginId, child);
            }
        } catch (IllegalArgumentException ex) {
            throw new PluginLoadException("Plugin jar " + jarPath + " has invalid permission declaration '" + permission.node() + "'", ex);
        }
    }

    static void validatePluginPermissionNode(String pluginId, String node) {
        var normalized = node == null ? "" : node.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("*") || !PERMISSION_NODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid permission node: " + node);
        }
        var base = normalized.endsWith(".*") ? normalized.substring(0, normalized.length() - 2) : normalized;
        for (var namespace : permissionNamespaces(pluginId)) {
            if (base.equals(namespace) || base.startsWith(namespace + ".")) {
                return;
            }
        }
        throw new IllegalArgumentException("Permission node '" + node + "' is outside plugin namespace '" + pluginId + "'");
    }

    private static List<String> permissionNamespaces(String pluginId) {
        var id = pluginId.toLowerCase(Locale.ROOT);
        var namespaces = new ArrayList<String>();
        namespaces.add(id);
        namespaces.add(id.replace('-', '.'));
        namespaces.add(id.replace("-", ""));
        var firstDash = id.indexOf('-');
        if (firstDash >= 0) {
            namespaces.add(id.substring(0, firstDash) + "." + id.substring(firstDash + 1).replace("-", ""));
        }
        return namespaces.stream().distinct().toList();
    }

    private SortResult sortArtifacts(List<PluginArtifact> artifacts) {
        var byId = new LinkedHashMap<String, PluginArtifact>();
        for (var artifact : artifacts) {
            byId.put(artifact.descriptor.id(), artifact);
        }

        var skipped = 0;
        if (options.continueOnLoadFailure()) {
            skipped += removeUnavailableDependencyArtifacts(byId);
            artifacts = List.copyOf(byId.values());
        }

        var incomingEdges = new LinkedHashMap<String, Integer>();
        var dependents = new LinkedHashMap<String, LinkedHashSet<String>>();
        for (var artifact : artifacts) {
            incomingEdges.put(artifact.descriptor.id(), 0);
            dependents.put(artifact.descriptor.id(), new LinkedHashSet<>());
        }
        for (var artifact : artifacts) {
            for (var dependency : artifact.descriptor.depends()) {
                if (!byId.containsKey(dependency)) {
                    if (options.continueOnLoadFailure()) {
                        continue;
                    }
                    throw new PluginLoadException("Plugin '" + artifact.descriptor.id() + "' depends on missing plugin '" + dependency + "'");
                }
                addLoadOrderEdge(dependency, artifact.descriptor.id(), incomingEdges, dependents);
            }
            for (var after : artifact.descriptor.loadAfter()) {
                if (byId.containsKey(after)) {
                    addLoadOrderEdge(after, artifact.descriptor.id(), incomingEdges, dependents);
                }
            }
            for (var before : artifact.descriptor.loadBefore()) {
                if (byId.containsKey(before)) {
                    addLoadOrderEdge(artifact.descriptor.id(), before, incomingEdges, dependents);
                }
            }
        }
        var ready = new ArrayDeque<PluginArtifact>();
        for (var artifact : artifacts) {
            if (incomingEdges.get(artifact.descriptor.id()) == 0) {
                ready.addLast(artifact);
            }
        }
        var ordered = new ArrayList<PluginArtifact>(artifacts.size());
        while (!ready.isEmpty()) {
            var artifact = ready.removeFirst();
            ordered.add(artifact);
            for (var dependent : dependents.get(artifact.descriptor.id())) {
                var remaining = incomingEdges.get(dependent) - 1;
                incomingEdges.put(dependent, remaining);
                if (remaining == 0) {
                    ready.addLast(byId.get(dependent));
                }
            }
        }
        if (ordered.size() != artifacts.size()) {
            if (!options.continueOnLoadFailure()) {
                throw new PluginLoadException("Plugin load order graph contains a cycle");
            }
            for (var artifact : artifacts) {
                if (!ordered.contains(artifact)) {
                    skipped++;
                    recordError(
                            artifact.descriptor.id(),
                            artifact.descriptor,
                            artifact.jarPath,
                            PluginLifecycle.ERROR,
                            "load",
                            new PluginLoadException("Plugin load order graph contains a cycle")
                    );
                    LOGGER.warn("Skipping plugin {} because the load order graph contains a cycle", artifact.descriptor.id());
                }
            }
        }
        return new SortResult(ordered, skipped);
    }

    private static void addLoadOrderEdge(
            String before,
            String after,
            LinkedHashMap<String, Integer> incomingEdges,
            LinkedHashMap<String, LinkedHashSet<String>> dependents
    ) {
        if (dependents.get(before).add(after)) {
            incomingEdges.put(after, incomingEdges.get(after) + 1);
        }
    }

    private int removeUnavailableDependencyArtifacts(LinkedHashMap<String, PluginArtifact> byId) {
        var skipped = 0;
        var changed = true;
        while (changed) {
            changed = false;
            var iterator = byId.values().iterator();
            while (iterator.hasNext()) {
                var artifact = iterator.next();
                var unavailableDependency = firstUnavailableDependency(artifact.descriptor.depends(), byId);
                if (unavailableDependency == null) {
                    continue;
                }
                skipped++;
                changed = true;
                iterator.remove();
                recordDependencyError(artifact, unavailableDependency, "load");
                LOGGER.warn(
                        "Skipping plugin {} because dependency {} is unavailable",
                        artifact.descriptor.id(),
                        unavailableDependency
                );
            }
        }
        return skipped;
    }

    private List<PluginClassLoader> dependencyClassLoaders(
            List<String> requiredIds,
            List<String> optionalIds
    ) {
        var visibleIds = new LinkedHashSet<>(requiredIds);
        visibleIds.addAll(optionalIds);
        var dependencies = new ArrayList<PluginClassLoader>(visibleIds.size());
        for (var dependencyId : visibleIds) {
            var loadedPlugin = loadedPlugins.get(dependencyId);
            if (loadedPlugin == null) {
                if (requiredIds.contains(dependencyId)) {
                    throw new PluginLoadException("Plugin '" + dependencyId + "' must be loaded before its dependents");
                }
                continue;
            }
            dependencies.add(loadedPlugin.classLoader);
        }
        return dependencies;
    }

    private String firstUnavailableDependency(List<String> dependencyIds) {
        for (var dependencyId : dependencyIds) {
            if (!loadedPlugins.containsKey(dependencyId)) {
                return dependencyId;
            }
        }
        return null;
    }

    private static String firstUnavailableDependency(
            List<String> dependencyIds,
            LinkedHashMap<String, PluginArtifact> availableArtifacts
    ) {
        for (var dependencyId : dependencyIds) {
            if (!availableArtifacts.containsKey(dependencyId)) {
                return dependencyId;
            }
        }
        return null;
    }

    private String firstDisabledDependency(List<String> dependencyIds) {
        for (var dependencyId : dependencyIds) {
            var loadedPlugin = loadedPlugins.get(dependencyId);
            if (loadedPlugin == null || !loadedPlugin.enabled) {
                return dependencyId;
            }
        }
        return null;
    }

    private Plugin instantiatePlugin(PluginDescriptor descriptor, PluginClassLoader classLoader) {
        try {
            var type = classLoader.loadClass(descriptor.mainClass());
            if (!Plugin.class.isAssignableFrom(type)) {
                throw new PluginLoadException("Plugin '" + descriptor.id() + "' main class does not implement Plugin");
            }
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (Plugin) constructor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException ex) {
            throw new PluginLoadException("Failed to construct plugin '" + descriptor.id() + "'", ex);
        } catch (InvocationTargetException ex) {
            throw new PluginLoadException("Plugin '" + descriptor.id() + "' constructor failed", ex.getTargetException());
        }
    }

    private static java.net.URL toJarUrl(Path jarPath) {
        try {
            return jarPath.toUri().toURL();
        } catch (MalformedURLException ex) {
            throw new PluginLoadException("Invalid plugin path " + jarPath, ex);
        }
    }

    private static ScoreboardService unavailableScoreboardService() {
        return new FandScoreboardService(() -> {
            throw new IllegalStateException("Minecraft server is not attached");
        });
    }

    private static PluginMessaging defaultPluginMessaging(PacketRegistry packetRegistry) {
        return packetRegistry instanceof PacketRegistryImpl impl
                ? new FandPluginMessaging(impl)
                : PluginMessaging.empty();
    }

    private void disablePlugin(LoadedPlugin loadedPlugin) {
        boolean wasEnabled = loadedPlugin.enabled;
        try {
            if (wasEnabled) {
                loadedPlugin.plugin.onDisable(loadedPlugin.context);
            }
        } catch (Throwable failure) {
            LOGGER.warn("Plugin {} failed during disable", loadedPlugin.descriptor.id(), failure);
        } finally {
            if (wasEnabled) {
                firePluginDisableEvent(loadedPlugin.descriptor);
            }
            loadedPlugin.enabled = false;
            closeQuietly(loadedPlugin.context, loadedPlugin.descriptor.id());
        }
    }

    private void firePluginEnableEvent(PluginDescriptor descriptor) {
        try {
            eventBus.fire(new PluginEnableEvent(descriptor));
        } catch (RuntimeException failure) {
            LOGGER.warn("PluginEnableEvent listener failed for {}", descriptor.id(), failure);
        }
    }

    private void firePluginDisableEvent(PluginDescriptor descriptor) {
        try {
            eventBus.fire(new PluginDisableEvent(descriptor));
        } catch (RuntimeException failure) {
            LOGGER.warn("PluginDisableEvent listener failed for {}", descriptor.id(), failure);
        }
    }

    private void discardLoadedPlugin(LoadedPlugin loadedPlugin) {
        loadedPlugin.enabled = false;
        loadedPlugins.remove(loadedPlugin.descriptor.id(), loadedPlugin);
        loadOrder.remove(loadedPlugin.descriptor.id());
        closeQuietly(loadedPlugin.context, loadedPlugin.descriptor.id());
        // Reclaim plugin-scoped permission declarations / registrations after the
        // owning plugin is gone, so a reload with a changed default does not trip
        // "Permission already registered with a different default".
        if (permissions instanceof io.fand.server.permission.PermissionManager manager) {
            manager.unregisterNamespaces(new java.util.HashSet<>(permissionNamespaces(loadedPlugin.descriptor.id())));
        }
        closeQuietly(loadedPlugin.classLoader, loadedPlugin.descriptor.id());
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Plugin runtime is closed");
        }
    }

    private static void closeQuietly(PluginClassLoader classLoader, String pluginId) {
        try {
            classLoader.close();
        } catch (IOException failure) {
            LOGGER.warn("Failed to close classloader for plugin {}", pluginId, failure);
        }
    }

    private static void closeQuietly(RuntimePluginContext context, String pluginId) {
        try {
            context.close();
        } catch (RuntimeException failure) {
            LOGGER.warn("Failed to close context for plugin {}", pluginId, failure);
        }
    }

    private record PluginArtifact(Path jarPath, PluginDescriptor descriptor) {
    }

    private record DiscoveryResult(List<PluginArtifact> artifacts, int skipped) {
    }

    private record SortResult(List<PluginArtifact> artifacts, int skipped) {
    }

    private static final class LoadedPlugin {

        private final PluginDescriptor descriptor;
        private final Path jarPath;
        private final Plugin plugin;
        private final RuntimePluginContext context;
        private final PluginClassLoader classLoader;
        private final PluginResourceTracker resources;
        private volatile boolean enabled;

        private LoadedPlugin(
                PluginDescriptor descriptor,
                Path jarPath,
                Plugin plugin,
                RuntimePluginContext context,
                PluginClassLoader classLoader,
                PluginResourceTracker resources
        ) {
            this.descriptor = descriptor;
            this.jarPath = jarPath;
            this.plugin = plugin;
            this.context = context;
            this.classLoader = classLoader;
            this.resources = resources;
        }
    }

    public enum PluginLifecycle {
        ENABLED,
        DISABLED,
        AVAILABLE,
        ERROR
    }

    public record PluginError(String phase, String message, long timestampMillis) {
    }

    public record PluginStatus(
            String id,
            PluginLifecycle lifecycle,
            boolean enabled,
            PluginDescriptor descriptor,
            Path jarPath,
            Path dataDirectory,
            List<String> dependencies,
            List<String> dependents,
            PluginError lastError,
            long loadedAtMillis,
            long enabledAtMillis,
            long disabledAtMillis,
            List<CommandInfo> commands,
            List<PermissionDescriptor> permissions
    ) {
        public PluginStatus {
            dependencies = List.copyOf(dependencies);
            dependents = List.copyOf(dependents);
            commands = List.copyOf(commands);
            permissions = List.copyOf(permissions);
        }
    }

    public record PluginOperationResult(boolean success, String message) {
        public static PluginOperationResult success(String message) {
            return new PluginOperationResult(true, message);
        }

        public static PluginOperationResult failure(String message) {
            return new PluginOperationResult(false, message);
        }
    }

    private static final class StatusEntry {

        private final String id;
        private volatile PluginLifecycle lifecycle = PluginLifecycle.AVAILABLE;
        private volatile PluginDescriptor descriptor;
        private volatile Path jarPath;
        private volatile PluginError lastError;
        private volatile long loadedAtMillis;
        private volatile long enabledAtMillis;
        private volatile long disabledAtMillis;

        private StatusEntry(String id) {
            this.id = id;
        }
    }

    private record DefaultCustomServices(FandCustomItemRegistry items, FandCustomBlockRegistry blocks) {
    }

    private record DescriptorFile(
            String id,
            String version,
            String mainClass,
            String description,
            String website,
            String license,
            String apiVersion,
            List<String> authors,
            List<String> depends,
            List<String> loadAfter,
            List<String> loadBefore,
            List<PermissionFile> permissions
    ) {
        private PluginDescriptor toDescriptor() {
            return new PluginDescriptor(
                    required(id, "id"),
                    required(version, "version"),
                    required(mainClass, "mainClass"),
                    optional(description),
                    optional(website),
                    optional(license),
                    apiVersion == null ? PluginDescriptor.CURRENT_API_VERSION : apiVersion.trim(),
                    authors == null ? List.of() : authors,
                    depends == null ? List.of() : depends,
                    loadAfter == null ? List.of() : loadAfter,
                    loadBefore == null ? List.of() : loadBefore,
                    permissions == null ? List.of() : permissions.stream().map(PermissionFile::toDescriptor).toList()
            );
        }

        private static String required(String value, String name) {
            if (value == null) {
                throw new PluginLoadException("Plugin descriptor is missing '" + name + "'");
            }
            return value.trim();
        }

        private static String optional(String value) {
            return value == null ? "" : value.trim();
        }
    }

    public record Options(
            boolean continueOnLoadFailure,
            boolean continueOnEnableFailure,
            boolean logSummary
    ) {
        public static Options defaults() {
            return new Options(true, true, true);
        }
    }

    private record PermissionFile(
            String node,
            String defaultAccess,
            Map<String, Boolean> children
    ) {
        private PermissionDescriptor toDescriptor() {
            return new PermissionDescriptor(
                    DescriptorFile.required(node, "permissions[].node"),
                    defaultAccess == null ? PermissionDefault.FALSE : PermissionDefault.valueOf(defaultAccess.trim().toUpperCase(Locale.ROOT)),
                    children == null ? Map.of() : children
            );
        }
    }
}
