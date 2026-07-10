package io.fand.server.plugin;

import io.fand.api.advancement.AdvancementRegistry;
import io.fand.api.auth.LoginAuthenticationService;
import io.fand.api.bossbar.BossBarService;
import io.fand.api.command.CommandRegistry;
import io.fand.api.config.Configuration;
import io.fand.api.config.ConfigurationService;
import io.fand.api.customblock.CustomBlockRegistry;
import io.fand.api.customitem.CustomItemRegistry;
import io.fand.api.datapack.DataPackService;
import io.fand.api.enchantment.EnchantmentRegistry;
import io.fand.api.event.EventBus;
import io.fand.api.gamerule.GameRuleService;
import io.fand.api.gui.GuiService;
import io.fand.api.hologram.HologramService;
import io.fand.api.integration.ExternalIntegrationStrategy;
import io.fand.api.localization.LocalizationService;
import io.fand.api.loot.LootTableService;
import io.fand.api.map.MapService;
import io.fand.api.messaging.PluginMessaging;
import io.fand.api.nms.NmsService;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.placeholder.PlaceholderService;
import io.fand.api.permission.PermissionService;
import io.fand.api.player.SimulatedPlayerService;
import io.fand.api.region.RegionService;
import io.fand.api.plugin.PluginContext;
import io.fand.api.plugin.PluginDescriptor;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.resourcepack.ResourcePackService;
import io.fand.api.scheduler.Scheduler;
import io.fand.api.scoreboard.ScoreboardService;
import io.fand.api.service.ServiceRegistry;
import io.fand.api.storage.PluginStorage;
import io.fand.api.structure.StructureService;
import io.fand.api.tablist.TabListService;
import io.fand.api.text.MiniMessageService;
import io.fand.server.config.FandConfigurationService;
import io.fand.server.config.YamlConfiguration;
import io.fand.server.localization.FandLocalizationService;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final LootTableService lootTables;
    private final AdvancementRegistry advancements;
    private final EnchantmentRegistry enchantments;
    private final StructureService structures;
    private final MapService maps;
    private final BossBarService bossBars;
    private final HologramService holograms;
    private final TabListService tabLists;
    private final SimulatedPlayerService simulatedPlayers;
    private final PlaceholderService placeholders;
    private final MiniMessageService miniMessages;
    private final ScoreboardService scoreboard;
    private final PacketRegistry packets;
    private final PluginMessaging pluginMessaging;
    private final GameRuleService gameRules;
    private final RegionService regions;
    private final DataPackService dataPacks;
    private final ResourcePackService resourcePacks;
    private final ExternalIntegrationStrategy integrations;
    private final ServiceRegistry services;
    private final NmsService nms;
    private final LoginAuthenticationService loginAuthenticators;
    private final CustomItemRegistry customItems;
    private final CustomBlockRegistry customBlocks;
    private final GuiService guis;
    private final Scheduler scheduler;
    private final Path dataDirectory;
    private final PluginResourceTracker resources;
    private final ClassLoader pluginClassLoader;
    private volatile YamlConfiguration config;
    private volatile PluginStorage storage;
    private volatile LocalizationService localization;
    private final AtomicBoolean closed = new AtomicBoolean();

    public RuntimePluginContext(
            PluginDescriptor descriptor,
            Logger logger,
            EventBus events,
            PermissionService permissions,
            CommandRegistry commands,
            RecipeRegistry recipes,
            LootTableService lootTables,
            AdvancementRegistry advancements,
            EnchantmentRegistry enchantments,
            StructureService structures,
            MapService maps,
            BossBarService bossBars,
            HologramService holograms,
            TabListService tabLists,
            SimulatedPlayerService simulatedPlayers,
            PlaceholderService placeholders,
            MiniMessageService miniMessages,
            ScoreboardService scoreboard,
            PacketRegistry packets,
            PluginMessaging pluginMessaging,
            GameRuleService gameRules,
            RegionService regions,
            DataPackService dataPacks,
            ResourcePackService resourcePacks,
            ExternalIntegrationStrategy integrations,
            ServiceRegistry services,
            NmsService nms,
            LoginAuthenticationService loginAuthenticators,
            CustomItemRegistry customItems,
            CustomBlockRegistry customBlocks,
            GuiService guis,
            Scheduler scheduler,
            Path dataDirectory,
            PluginResourceTracker resources,
            ClassLoader pluginClassLoader
    ) {
        this.descriptor = descriptor;
        this.logger = logger;
        this.events = events;
        this.permissions = new PluginPermissionService(permissions, resources, descriptor.id());
        this.commands = commands;
        this.recipes = recipes;
        this.lootTables = lootTables;
        this.advancements = advancements;
        this.enchantments = enchantments;
        this.structures = structures;
        this.maps = maps;
        this.bossBars = bossBars;
        this.holograms = holograms;
        this.tabLists = tabLists;
        this.simulatedPlayers = simulatedPlayers;
        this.placeholders = placeholders;
        this.miniMessages = miniMessages;
        this.scoreboard = scoreboard;
        this.packets = packets;
        this.pluginMessaging = pluginMessaging;
        this.gameRules = gameRules;
        this.regions = regions;
        this.dataPacks = dataPacks;
        this.resourcePacks = resourcePacks;
        this.integrations = integrations;
        this.services = services;
        this.nms = nms;
        this.loginAuthenticators = loginAuthenticators;
        this.customItems = customItems;
        this.customBlocks = customBlocks;
        this.guis = guis;
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
    public LootTableService lootTables() {
        return lootTables;
    }

    @Override
    public AdvancementRegistry advancements() {
        return advancements;
    }

    @Override
    public EnchantmentRegistry enchantments() {
        return enchantments;
    }

    @Override
    public StructureService structures() {
        return structures;
    }

    @Override
    public MapService maps() {
        return maps;
    }

    @Override
    public BossBarService bossBars() {
        return bossBars;
    }

    @Override
    public HologramService holograms() {
        return holograms;
    }

    @Override
    public TabListService tabLists() {
        return tabLists;
    }

    @Override
    public SimulatedPlayerService simulatedPlayers() {
        return simulatedPlayers;
    }

    @Override
    public PlaceholderService placeholders() {
        return placeholders;
    }

    @Override
    public MiniMessageService miniMessages() {
        return miniMessages;
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
    public PluginMessaging pluginMessaging() {
        return pluginMessaging;
    }

    @Override
    public GameRuleService gameRules() {
        return gameRules;
    }

    @Override
    public RegionService regions() {
        return regions;
    }

    @Override
    public DataPackService dataPacks() {
        return dataPacks;
    }

    @Override
    public ResourcePackService resourcePacks() {
        return resourcePacks;
    }

    @Override
    public LocalizationService localization() {
        var existing = localization;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (localization == null) {
                localization = new FandLocalizationService(
                        dataDirectory(),
                        LocalizationService.DEFAULT_LOCALE,
                        placeholders,
                        miniMessages,
                        pluginClassLoader);
            }
            return localization;
        }
    }

    @Override
    public ExternalIntegrationStrategy integrations() {
        return integrations;
    }

    @Override
    public ServiceRegistry services() {
        return services;
    }

    @Override
    public NmsService nms() {
        return nms;
    }

    @Override
    public LoginAuthenticationService loginAuthenticators() {
        return loginAuthenticators;
    }

    @Override
    public CustomItemRegistry customItems() {
        return customItems;
    }

    @Override
    public CustomBlockRegistry customBlocks() {
        return customBlocks;
    }

    @Override
    public GuiService guis() {
        return guis;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public PluginStorage storage() {
        var existing = storage;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (storage == null) {
                storage = new FandPluginStorage(dataDirectory());
            }
            return storage;
        }
    }

    @Override
    public ConfigurationService configurations() {
        return FandConfigurationService.INSTANCE;
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
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        var existingStorage = storage;
        if (existingStorage != null) {
            try {
                existingStorage.flush();
            } catch (RuntimeException ex) {
                logger.warn("Failed to flush storage for plugin {}", descriptor.id(), ex);
            }
            if (existingStorage instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ex) {
                    logger.warn("Failed to close storage for plugin {}", descriptor.id(), ex);
                }
            }
        }
        try {
            resources.close();
        } catch (RuntimeException ex) {
            logger.warn("Failed to close plugin resources for plugin {}", descriptor.id(), ex);
        }
    }
}
