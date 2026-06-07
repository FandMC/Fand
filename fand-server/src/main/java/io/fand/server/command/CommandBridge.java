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
        var sender = sender(source); // 保留您自己的逻辑：支持玩家识别
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

    // 融合：使用官方新版的返回值 SuggestionResult，但将 sender 替换为您自己的 sender(source)，确保补全也能正确识别玩家
    public static Optional<SuggestionResult> suggestions(CommandSourceStack source, String rawCommand) {
        var runtime = io.fand.server.Main.runtime();
        var sender = sender(source); // 保留：支持玩家识别
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

    // 完全保留：您自己新写的核心方法（识别是玩家还是控制台）
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

    // 保留：官方新增的前缀去除方法
    private static NormalizedCommand stripCommandPrefix(String rawCommand) {
        var leading = rawCommand.length() - rawCommand.stripLeading().length();
        return rawCommand.regionMatches(leading, "/", 0, 1)
                ? new NormalizedCommand(rawCommand.substring(0, leading + 1).length(), rawCommand.substring(leading + 1))
                : new NormalizedCommand(leading, rawCommand.substring(leading));
    }

    // 保留：官方新增的 Token 计算方法
    private static int currentTokenStart(String command) {
        var trimmed = command.stripTrailing();
        if (trimmed.length() != command.length()) {
            return command.length();
        }
        var separator = Math.max(command.lastIndexOf(' '), command.lastIndexOf('\t'));
        return separator < 0 ? 0 : separator + 1;
    }

    // 保留：官方新增的 Record
    public record SuggestionResult(List<String> values, int replaceStart, int replaceEnd) {
    }

    // 保留：官方新增的 Record
    private record NormalizedCommand(int prefixLength, String command) {
    }
}