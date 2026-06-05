package io.fand.testplugin;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.api.lifecycle.ServerStartedEvent;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginContext;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

public final class TestPlugin implements Plugin {

    @Override
    public void onLoad(PluginContext context) {
        context.logger().info("test-plugin loaded");
    }

    @Override
    public void onEnable(PluginContext context) {
        context.commands().register(new HelloCommand(context));
        context.commands().register(new TeleportCommand(context));
        context.commands().register(new SetBlockCommand(context));
        context.events().subscribe(ServerStartedEvent.class, event ->
                context.logger().info("Server started; Fand brand={} version={}",
                        event.server().brand(), event.server().version()));
        context.events().subscribe(PlayerJoinEvent.class, event -> {
            context.logger().info("{} joined ({} online)", event.player().name(),
                    event.player().online() ? "still" : "now offline");
            event.player().sendMessage(Component.text("Welcome from test-plugin!", NamedTextColor.AQUA));
        });
        context.events().registerListener(new PlayerEvents(context.logger()));
        context.scheduler().runMainRepeating(
                () -> context.logger().debug("test-plugin heartbeat"),
                Duration.ofSeconds(30),
                Duration.ofSeconds(60));
        context.logger().info("test-plugin enabled");
    }

    @Override
    public void onDisable(PluginContext context) {
        context.logger().info("test-plugin disabled");
    }

    @CommandSpec(label = "fandtest", permission = "fand.testplugin.use")
    static final class HelloCommand implements CommandExecutor {

        private final PluginContext context;

        HelloCommand(PluginContext context) {
            this.context = context;
        }

        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
            var greeting = args.isEmpty() ? sender.name() : String.join(" ", args);
            sender.sendMessage(Component.text("Hello, " + greeting + "!", NamedTextColor.GREEN));
            context.logger().info("/fandtest invoked by {} with args {}", sender.name(), args);
        }
    }

    static final class PlayerEvents implements Listener {

        private final Logger logger;

        PlayerEvents(Logger logger) {
            this.logger = logger;
        }

        @Subscribe
        public void onQuit(PlayerQuitEvent event) {
            logger.info("{} left", event.player().name());
        }
    }

    @CommandSpec(label = "fandtp", permission = "fand.testplugin.tp")
    static final class TeleportCommand implements CommandExecutor {

        private final PluginContext context;

        TeleportCommand(PluginContext context) {
            this.context = context;
        }

        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("/fandtp must be run by a player", NamedTextColor.RED));
                return;
            }
            if (args.size() != 3) {
                sender.sendMessage(Component.text("Usage: /fandtp <x> <y> <z>", NamedTextColor.RED));
                return;
            }
            double x;
            double y;
            double z;
            try {
                x = Double.parseDouble(args.get(0));
                y = Double.parseDouble(args.get(1));
                z = Double.parseDouble(args.get(2));
            } catch (NumberFormatException ex) {
                sender.sendMessage(Component.text("Coordinates must be numbers", NamedTextColor.RED));
                return;
            }
            var destination = player.world().at(x, y, z);
            player.teleport(destination).whenComplete((ok, failure) -> {
                if (failure != null) {
                    context.logger().warn("Teleport failed for {}", player.name(), failure);
                    return;
                }
                if (Boolean.TRUE.equals(ok)) {
                    player.sendMessage(Component.text("Teleported.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Teleport rejected.", NamedTextColor.YELLOW));
                }
            });
        }
    }

    @CommandSpec(label = "fandsetblock", permission = "fand.testplugin.setblock")
    static final class SetBlockCommand implements CommandExecutor {

        private final PluginContext context;

        SetBlockCommand(PluginContext context) {
            this.context = context;
        }

        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("/fandsetblock must be run by a player", NamedTextColor.RED));
                return;
            }
            if (args.size() != 1) {
                sender.sendMessage(Component.text("Usage: /fandsetblock <type>", NamedTextColor.RED));
                return;
            }
            io.fand.api.block.BlockType type;
            try {
                type = io.fand.api.block.BlockTypes.of(args.get(0));
            } catch (java.util.NoSuchElementException ex) {
                sender.sendMessage(Component.text("Unknown block: " + args.get(0), NamedTextColor.RED));
                return;
            } catch (net.kyori.adventure.key.InvalidKeyException ex) {
                sender.sendMessage(Component.text("Invalid block key: " + args.get(0), NamedTextColor.RED));
                return;
            }
            var loc = player.location();
            var block = player.world().blockAt((int) Math.floor(loc.x()), (int) Math.floor(loc.y()) - 1, (int) Math.floor(loc.z()));
            block.setType(type);
            player.sendMessage(Component.text("Set " + type.key().asString() + " at " + block.x() + "," + block.y() + "," + block.z(), NamedTextColor.GREEN));
            context.logger().info("/fandsetblock by {} set {} at {},{},{}", player.name(), type.key(), block.x(), block.y(), block.z());
        }
    }
}
