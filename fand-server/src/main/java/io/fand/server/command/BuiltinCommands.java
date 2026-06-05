package io.fand.server.command;

import io.fand.api.command.CommandRegistry;

public final class BuiltinCommands {

    private BuiltinCommands() {
    }

    public static void registerAll(CommandRegistry commands, io.fand.server.FandServer server) {
        commands.register(new FandCommand(server));
    }
}
