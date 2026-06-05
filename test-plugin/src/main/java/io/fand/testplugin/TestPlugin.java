package io.fand.testplugin;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.api.lifecycle.ServerStartedEvent;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginContext;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class TestPlugin implements Plugin {

    @Override
    public void onLoad(PluginContext context) {
        context.logger().info("test-plugin loaded");
    }

    @Override
    public void onEnable(PluginContext context) {
        context.commands().register(new HelloCommand(context));
        context.events().subscribe(ServerStartedEvent.class, event ->
                context.logger().info("Server started; Fand brand={} version={}",
                        event.server().brand(), event.server().version()));
        context.events().subscribe(PlayerJoinEvent.class, event -> {
            context.logger().info("{} joined ({} online)", event.player().name(),
                    event.player().online() ? "still" : "now offline");
            event.player().sendMessage(Component.text("Welcome from test-plugin!", NamedTextColor.AQUA));
        });
        context.events().subscribe(PlayerQuitEvent.class, event ->
                context.logger().info("{} left", event.player().name()));
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
}
