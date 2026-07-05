package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.Server;
import io.fand.api.command.CommandSender;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandtest", arguments = {"greeting"}, aliases = {"ftest"}, permission = "fand.testplugin.use")
final class HelloCommand implements TestCommandHandler {

    private final PluginContext context;

    HelloCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        var greeting = args.isEmpty() ? sender.name() : String.join(" ", args);
        sender.sendMessage(Component.text(message(context.config(), "messages.greeting", "Hello") + ", " + greeting + "!", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Server: " + Fand.server().brand() + " " + Fand.server().version(), NamedTextColor.GRAY));
        context.logger().info("/fandtest invoked by {} with args {}", sender.name(), args);
    }
}
