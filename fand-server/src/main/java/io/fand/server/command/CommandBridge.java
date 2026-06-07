package io.fand.server.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class CommandBridge {

    private CommandBridge() {
    }

    public static boolean tryExecute(CommandSourceStack source, String rawCommand) {
        var sender = CommandEvents.sender(source);
        var tokens = tokenize(rawCommand, false);
        if (tokens.isEmpty()) {
            return false;
        }

        var runtime = io.fand.server.Main.runtime();
        var registry = runtime.commands();
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
            CommandEvents.runInCommandContext(() -> {
                try {
                    command.command().executor().execute(sender, command.usedLabel(), args);
                } catch (Exception failure) {
                    throw new CommandExecutionFailure(failure);
                }
            });
        } catch (CommandExecutionFailure failure) {
            var cause = failure.getCause();
            var message = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
            source.sendFailure(Component.literal("Fand command failed: " + message));
        } catch (Exception failure) {
            var message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            source.sendFailure(Component.literal("Fand command failed: " + message));
        }
        return true;
    }

    public static Optional<SuggestionResult> suggestions(CommandSourceStack source, String rawCommand) {
        var runtime = io.fand.server.Main.runtime();
        var sender = new CommandSourceSender(source, runtime.permissions());
        var normalized = stripCommandPrefix(rawCommand);
        var tokens = tokenize(normalized.command(), true);
        var registry = runtime.commands();
        if (!registry.claims(tokens)) {
            return Optional.empty();
        }
        return Optional.of(new SuggestionResult(
                registry.suggestions(sender, tokens),
                normalized.prefixLength() + currentTokenStart(normalized.command()),
                rawCommand.length()
        ));
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

    private static NormalizedCommand stripCommandPrefix(String rawCommand) {
        var leading = rawCommand.length() - rawCommand.stripLeading().length();
        return rawCommand.regionMatches(leading, "/", 0, 1)
                ? new NormalizedCommand(rawCommand.substring(0, leading + 1).length(), rawCommand.substring(leading + 1))
                : new NormalizedCommand(leading, rawCommand.substring(leading));
    }

    private static int currentTokenStart(String command) {
        var trimmed = command.stripTrailing();
        if (trimmed.length() != command.length()) {
            return command.length();
        }
        var separator = Math.max(command.lastIndexOf(' '), command.lastIndexOf('\t'));
        return separator < 0 ? 0 : separator + 1;
    }

    public record SuggestionResult(List<String> values, int replaceStart, int replaceEnd) {
    }

    private record NormalizedCommand(int prefixLength, String command) {
    }

    private static final class CommandExecutionFailure extends RuntimeException {

        private CommandExecutionFailure(Throwable cause) {
            super(cause);
        }
    }
}
