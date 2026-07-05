package io.fand.server.command;

import io.fand.api.command.CommandRegistry;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import java.util.List;

public final class BuiltinCommands {

    private BuiltinCommands() {
    }

    public static void registerAll(CommandRegistry commands, io.fand.server.FandServer server) {
        commands.register(new FandCommand(server));
        commands.register(new TpsCommand(server));
        commands.register(new MsptCommand(server));
        commands.register(new PluginsCommand(server));
        new PluginCommand(server).register(commands);
        registerPluginCommandPermissions(server);
    }

    private static void registerPluginCommandPermissions(io.fand.server.FandServer server) {
        for (var node : List.of(
                "fand.command.plugin.list",
                "fand.command.plugin.info",
                "fand.command.plugin.status",
                "fand.command.plugin.load",
                "fand.command.plugin.unload",
                "fand.command.plugin.reload",
                "fand.command.plugin.enable",
                "fand.command.plugin.disable",
                "fand.command.plugin.depends",
                "fand.command.plugin.dependents",
                "fand.command.plugin.commands",
                "fand.command.plugin.permissions",
                "fand.command.plugin.errors"
        )) {
            server.permissions().register(new PermissionDescriptor(node, PermissionDefault.OPERATOR));
        }
    }
}
