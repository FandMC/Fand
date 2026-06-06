package io.fand.server.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CommandBridge {

    private CommandBridge() {
    }

    public static boolean tryExecute(CommandSourceStack source, String rawCommand) {
        var sender = sender(source);
        var tokens = tokenize(rawCommand, false);
        if (tokens.isEmpty()) {
            return false;
        }

        var registry = io.fand.server.Main.runtime().commands();
        var resolved = registry.resolve(sender, tokens);
        if (resolved.isEmpty()) {
            if (registry.claims(tokens)) {
                source.sendFailure(Component.literal("Unknown Fand command."));
                return true;
            }
            return false;
        }

        var command = resolved.get();
        var args = tokens.size() <= command.matchedLength() ? List.<String>of() : tokens.subList(command.matchedLength(), tokens.size());
        try {
            command.command().executor().execute(sender, command.usedLabel(), args);
        } catch (Exception failure) {
            var message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            source.sendFailure(Component.literal("Fand command failed: " + message));
        }
        return true;
    }

    public static Optional<List<String>> suggestions(CommandSourceStack source, String rawCommand) {
        var sender = sender(source);
        var tokens = tokenize(rawCommand, true);
        var registry = io.fand.server.Main.runtime().commands();
        if (!registry.claims(tokens)) {
            return Optional.empty();
        }
        return Optional.of(registry.suggestions(sender, tokens));
    }

    public static io.fand.api.command.CommandSender sender(CommandSourceStack source) {
        var runtime = io.fand.server.Main.runtime();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            return runtime.playerRegistry()
                    .find(player.getUUID())
                    .orElseGet(() -> runtime.playerRegistry().attach(player));
        }
        return new CommandSourceSender(source, runtime.permissions());
    }

    private static List<String> tokenize(String rawCommand, boolean preserveTrailingArgument) {
        var trimmedLeading = rawCommand.stripLeading();
        if (trimmedLeading.isEmpty()) {
            return List.of();
        }
        var endsWithSpace = preserveTrailingArgument && Character.isWhitespace(rawCommand.charAt(rawCommand.length() - 1));
        var base = new ArrayList<String>(List.of(trimmedLeading.trim().split("\\s+")));
        if (endsWithSpace) {
            base.add("");
        }
        return List.copyOf(base);
    }
}
