package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.Server;
import io.fand.api.player.PlayerProfile;
import io.fand.api.advancement.CustomAdvancement;
import io.fand.api.enchantment.CustomEnchantment;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.world.ChunkLoadEvent;
import io.fand.api.event.world.ChunkUnloadEvent;
import io.fand.api.event.world.ThunderChangeEvent;
import io.fand.api.event.world.WeatherChangeEvent;
import io.fand.api.event.world.WorldLoadEvent;
import io.fand.api.event.world.WorldSaveEvent;
import io.fand.api.event.world.WorldUnloadEvent;
import io.fand.api.lifecycle.ServerStartedEvent;
import io.fand.api.auth.LoginAuthenticationRequest;
import io.fand.api.auth.LoginAuthenticationResult;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginContext;
import io.fand.api.world.World;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class TestPlugin implements Plugin {

    private static final Pattern ASCII_USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    @Override
    public void onLoad(PluginContext context) {
        context.logger().info("test-plugin loaded; data directory={}", context.dataDirectory());
    }

    @Override
    public void onEnable(PluginContext context) {
        Set<UUID> demoGuiViewers = ConcurrentHashMap.newKeySet();
        var nmsDemo = new NmsDemo(context);
        registerPermissions(context);
        context.loginAuthenticators().register(
                Key.key("fand-test-plugin:offline-cn"),
                TestPlugin::authenticateOfflineForNonAsciiName);
        nmsDemo.registerHooks();
        TestCommands.register(context.commands(), new HelloCommand(context));
        TestCommands.register(context.commands(), new DemoCommand(context));
        TestCommands.register(context.commands(), new KitCommand(context, demoGuiViewers));
        TestCommands.register(context.commands(), new PerformanceCommand());
        TestCommands.register(context.commands(), new WorldCommand(context));
        TestCommands.register(context.commands(), new TeleportCommand(context));
        TestCommands.register(context.commands(), new SpawnEntityCommand(context));
        TestCommands.register(context.commands(), new DropItemCommand(context));
        TestCommands.register(context.commands(), new SetBlockCommand(context));
        TestCommands.register(context.commands(), new GiveCommand(context));
        TestCommands.register(context.commands(), new ComponentItemCommand(context));
        TestCommands.register(context.commands(), new HealCommand(context));
        TestCommands.register(context.commands(), new GameModeCommand());
        TestCommands.register(context.commands(), new FlyCommand());
        TestCommands.register(context.commands(), new ActionBarCommand(context));
        TestCommands.register(context.commands(), new TitleCommand(context));
        TestCommands.register(context.commands(), new BossBarCommand(context));
        TestCommands.register(context.commands(), new ParticleCommand());
        TestCommands.register(context.commands(), new SoundCommand());
        TestCommands.register(context.commands(), new KickCommand(context));
        TestCommands.register(context.commands(), new TabCommand(context));
        TestCommands.register(context.commands(), new RecipeCommand(context));
        TestCommands.register(context.commands(), new ComponentsCommand());
        TestCommands.register(context.commands(), new SelfTestCommand(context));
        TestCommands.register(context.commands(), new NmsCommand(nmsDemo));
        TestCommands.register(context.commands(), new GuiCommand(context, demoGuiViewers));
        registerDemoRecipes(context);
        registerCustomRegistries(context);
        context.events().subscribe(ServerStartedEvent.class, event -> {
            context.logger().info("Server started; Fand brand={} version={} minecraft={}",
                    event.server().brand(), event.server().version(), event.server().minecraftVersion());
            nmsDemo.runStartupSelfTest(event);
        });
        context.events().subscribe(WorldLoadEvent.class, event ->
                context.logger().info("World loaded: {}", event.world().key().asString()));
        context.events().subscribe(WorldUnloadEvent.class, event ->
                context.logger().info("World unloading: {}", event.world().key().asString()));
        context.events().subscribe(WorldSaveEvent.class, event -> {
            if (context.config().booleanValue("features.log-world-events", true)) {
                context.logger().info("World saving: {}", event.world().key().asString());
            }
        });
        context.events().subscribe(WeatherChangeEvent.class, event -> {
            if (context.config().booleanValue("features.log-world-events", true)) {
                context.logger().info("Weather changed in {}: storm {} -> {}",
                        event.world().key().asString(), event.fromStorm(), event.toStorm());
            }
        });
        context.events().subscribe(ThunderChangeEvent.class, event -> {
            if (context.config().booleanValue("features.log-world-events", true)) {
                context.logger().info("Thunder changed in {}: thunder {} -> {}",
                        event.world().key().asString(), event.fromThundering(), event.toThundering());
            }
        });
        context.events().subscribe(ChunkLoadEvent.class, event -> {
            if (context.config().booleanValue("features.log-chunk-events", false)) {
                context.logger().info("Chunk loaded: {} [{},{}]",
                        event.world().key().asString(), event.chunkX(), event.chunkZ());
            }
        });
        context.events().subscribe(ChunkUnloadEvent.class, event -> {
            if (context.config().booleanValue("features.log-chunk-events", false)) {
                context.logger().info("Chunk unloaded: {} [{},{}]",
                        event.world().key().asString(), event.chunkX(), event.chunkZ());
            }
        });
        context.events().subscribe(PlayerJoinEvent.class, event -> {
            context.logger().info("{} joined ({} online players)", event.player().name(), Fand.server().onlinePlayers());
            var welcome = message(context.config(), "messages.welcome", "Welcome from test-plugin, {player}!");
            if (!welcome.isBlank()) {
                event.player().sendMessage(Component.text(welcome.replace("{player}", event.player().name()), NamedTextColor.AQUA));
            }
        });
        PlayerEvents.registerAll(context, demoGuiViewers);
        context.scheduler().runAsync(() -> context.logger().info("test-plugin config file={}", context.config().file()));
        context.scheduler().runMainRepeating(
                () -> context.logger().debug("test-plugin heartbeat; online={}", Fand.server().onlinePlayers()),
                Duration.ofSeconds(30),
                Duration.ofSeconds(60));
        context.logger().info("test-plugin enabled");
    }

    @Override
    public void onDisable(PluginContext context) {
        context.logger().info("test-plugin disabled");
    }

    private static void registerCustomRegistries(PluginContext context) {
        var quickening = context.enchantments().register(new CustomEnchantment(
                Key.key("fand-test-plugin:quickening"),
                Component.text("Quickening"),
                3));
        var mercy = context.enchantments().register(new CustomEnchantment(
                MERCY_ENCHANTMENT,
                Component.text("仁慈"),
                1));
        var plunder = context.enchantments().register(new CustomEnchantment(
                PLUNDER_ENCHANTMENT,
                Component.text("掠夺"),
                1));
        var firstStep = context.advancements().register(new CustomAdvancement(
                Key.key("fand-test-plugin:first_step"),
                Component.text("First Step"),
                Component.text("Registered by the Fand test plugin"),
                List.of("done")));

        context.logger().info("Registered custom enchantments: {}={} {}={} {}={}",
                quickening.key().asString(), quickening.active(),
                mercy.key().asString(), mercy.active(),
                plunder.key().asString(), plunder.active());
        context.logger().info("Registered custom advancement {} active={}",
                firstStep.key().asString(), firstStep.active());
    }

    static LoginAuthenticationResult authenticateOfflineForNonAsciiName(LoginAuthenticationRequest request) {
        if (ASCII_USERNAME.matcher(request.name()).matches()) {
            return LoginAuthenticationResult.pass();
        }
        return LoginAuthenticationResult.allow(
                new PlayerProfile(
                        request.requestedProfileId().orElseGet(() -> UUID.nameUUIDFromBytes(request.name().getBytes(java.nio.charset.StandardCharsets.UTF_8))),
                        request.name()));
    }
}
