package io.fand.server.command;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import java.util.List;

@CommandSpec(label = "fand", namespace = "fand", subcommands = {"reload"}, arguments = {}, permission = "fand.command.reload")
public final class FandCommand implements CommandExecutor {

    private final io.fand.server.FandServer server;

    public FandCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        var result = server.reloadConfig();
        for (var line : io.fand.server.config.ConfigReloadMessages.lines(result)) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(line));
        }
    }
}
