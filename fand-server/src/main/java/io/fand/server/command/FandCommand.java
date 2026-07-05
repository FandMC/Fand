package io.fand.server.command;

import io.fand.api.command.Command;
import io.fand.api.command.CommandContext;
import io.fand.api.command.Permission;
import io.fand.api.command.Subcommand;

@Command(value = "fand", namespace = "fand")
public final class FandCommand {

    private final io.fand.server.FandServer server;

    public FandCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    @Subcommand("reload")
    @Permission("fand.command.reload")
    public void reload(CommandContext context) {
        var result = server.reloadConfig();
        for (var line : io.fand.server.config.ConfigReloadMessages.lines(result)) {
            context.sender().sendMessage(net.kyori.adventure.text.Component.text(line));
        }
    }
}
