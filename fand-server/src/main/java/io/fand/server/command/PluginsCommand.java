package io.fand.server.command;

import io.fand.api.command.Aliases;
import io.fand.api.command.Command;
import io.fand.api.command.CommandContext;
import io.fand.api.command.Default;
import io.fand.api.command.Permission;

@Command(value = "plugins", namespace = "fand")
@Aliases("pl")
@Permission("fand.command.plugins")
public final class PluginsCommand {

    private final io.fand.server.FandServer server;

    public PluginsCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    @Default
    public void execute(CommandContext context) {
        PluginCommand.sendPluginList(context.sender(), server.pluginRuntime(), true);
    }
}
