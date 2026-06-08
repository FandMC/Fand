package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.Server;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.world.ChunkLoadEvent;
import io.fand.api.event.world.ChunkUnloadEvent;
import io.fand.api.event.world.ThunderChangeEvent;
import io.fand.api.event.world.WeatherChangeEvent;
import io.fand.api.event.world.WorldLoadEvent;
import io.fand.api.event.world.WorldSaveEvent;
import io.fand.api.event.world.WorldUnloadEvent;
import io.fand.api.lifecycle.ServerStartedEvent;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginContext;
import io.fand.api.world.World;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class TestPlugin implements Plugin {

    @Override
    public void onLoad(PluginContext context) {
        context.logger().info("test-plugin loaded; data directory={}", context.dataDirectory());
    }

    @Override
    public void onEnable(PluginContext context) {
        Set<UUID> demoGuiViewers = ConcurrentHashMap.newKeySet();
        registerPermissions(context);
        context.commands().register(new HelloCommand(context));
        context.commands().register(new DemoCommand(context));
        context.commands().register(new KitCommand(context, demoGuiViewers));
        context.commands().register(new PerformanceCommand());
        context.commands().register(new WorldCommand(context));
        context.commands().register(new TeleportCommand(context));
        context.commands().register(new SpawnEntityCommand(context));
        context.commands().register(new DropItemCommand(context));
        context.commands().register(new SetBlockCommand(context));
        context.commands().register(new GiveCommand(context));
        context.commands().register(new ComponentItemCommand(context));
        context.commands().register(new HealCommand(context));
        context.commands().register(new GameModeCommand());
        context.commands().register(new FlyCommand());
        context.commands().register(new ActionBarCommand(context));
        context.commands().register(new TitleCommand(context));
        context.commands().register(new BossBarCommand(context));
        context.commands().register(new ParticleCommand());
        context.commands().register(new SoundCommand());
        context.commands().register(new KickCommand(context));
        context.commands().register(new TabCommand(context));
        context.commands().register(new SidebarCommand());
        context.commands().register(new RecipeCommand(context));
        context.commands().register(new ComponentsCommand());
        context.commands().register(new SelfTestCommand(context));
        context.commands().register(new GuiCommand(context, demoGuiViewers));
        registerDemoRecipes(context);
        context.events().subscribe(ServerStartedEvent.class, event ->
                context.logger().info("Server started; Fand brand={} version={} minecraft={}",
                        event.server().brand(), event.server().version(), event.server().minecraftVersion()));
        context.events().subscribe(WorldLoadEvent.class, event ->
                context.logger().info("World loaded: {}", event.world().key().asString()));
        context.events().subscribe(WorldUnloadEvent.class, event ->
                context.logger().info("World unloading: {}", event.world().key().asString()));
        context.events().subscribe(WorldSaveEvent.class, event -> {
            if (context.config().getBoolean("features.log-world-events", true)) {
                context.logger().info("World saving: {}", event.world().key().asString());
            }
        });
        context.events().subscribe(WeatherChangeEvent.class, event -> {
            if (context.config().getBoolean("features.log-world-events", true)) {
                context.logger().info("Weather changed in {}: storm {} -> {}",
                        event.world().key().asString(), event.fromStorm(), event.toStorm());
            }
        });
        context.events().subscribe(ThunderChangeEvent.class, event -> {
            if (context.config().getBoolean("features.log-world-events", true)) {
                context.logger().info("Thunder changed in {}: thunder {} -> {}",
                        event.world().key().asString(), event.fromThundering(), event.toThundering());
            }
        });
        context.events().subscribe(ChunkLoadEvent.class, event -> {
            if (context.config().getBoolean("features.log-chunk-events", false)) {
                context.logger().info("Chunk loaded: {} [{},{}]",
                        event.world().key().asString(), event.chunkX(), event.chunkZ());
            }
        });
        context.events().subscribe(ChunkUnloadEvent.class, event -> {
            if (context.config().getBoolean("features.log-chunk-events", false)) {
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
}
