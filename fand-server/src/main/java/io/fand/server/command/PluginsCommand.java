package io.fand.server.command;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import java.util.List;

@CommandSpec(label = "plugins", namespace = "fand", aliases = {"pl"}, arguments = {}, permission = "fand.command.plugins")
public final class PluginsCommand implements CommandExecutor {

    private final io.fand.server.FandServer server;

    public PluginsCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        PluginCommand.sendPluginList(sender, server.pluginRuntime(), true);
    }
}
