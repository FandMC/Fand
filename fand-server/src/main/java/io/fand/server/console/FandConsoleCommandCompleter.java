package io.fand.server.console;

import com.mojang.brigadier.suggestion.Suggestion;
import io.fand.server.Main;
import io.fand.server.command.CommandBridge;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.dedicated.DedicatedServer;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FandConsoleCommandCompleter implements Completer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandConsoleCommandCompleter.class);

    private final DedicatedServer server;

    FandConsoleCommandCompleter(DedicatedServer server) {
        this.server = server;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        var fullLine = line.line();
        var cursor = Math.max(0, Math.min(line.cursor(), fullLine.length()));
        var input = fullLine.substring(0, cursor);
        var source = server.createCommandSourceStack();
        var suggestions = new LinkedHashSet<String>();
        addMinecraftSuggestions(source, fullLine, input, suggestions);
        if (Main.runtimeOrNull() != null) {
            addFandSuggestions(source, fullLine, input, suggestions);
        }
        rootLocalFirst(input, List.copyOf(suggestions)).forEach(value -> candidates.add(new Candidate(value)));
    }

    private void addMinecraftSuggestions(CommandSourceStack source, String fullLine, String input, LinkedHashSet<String> suggestions) {
        var normalized = normalize(input);
        var dispatcher = server.getCommands().getDispatcher();
        var parse = dispatcher.parse(normalized.command(), source);
        try {
            for (Suggestion suggestion : dispatcher.getCompletionSuggestions(parse).get().getList()) {
                suggestions.add(toMinecraftCandidate(fullLine, normalized, suggestion));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            LOGGER.warn("Failed to complete console command '{}'", input, ex);
        }
    }

    private static void addFandSuggestions(CommandSourceStack source, String fullLine, String input, LinkedHashSet<String> suggestions) {
        CommandBridge.suggestions(source, input)
                .ifPresent(result -> result.values().forEach(value -> suggestions.add(toFandCandidate(fullLine, result, value))));
    }

    static String toMinecraftCandidate(String fullLine, NormalizedCommand normalized, Suggestion suggestion) {
        var replacement = suggestion.getText();
        if (!normalized.hasSlash() && replacement.startsWith("/")) {
            replacement = replacement.substring(1);
        }
        var start = Math.min(normalized.prefixLength() + suggestion.getRange().getStart(), fullLine.length());
        var end = Math.min(normalized.prefixLength() + suggestion.getRange().getEnd(), fullLine.length());
        return fullLine.substring(0, start) + replacement + fullLine.substring(end);
    }

    static String toFandCandidate(String fullLine, CommandBridge.SuggestionResult result, String value) {
        var start = Math.min(result.replaceStart(), fullLine.length());
        var end = Math.min(result.replaceEnd(), fullLine.length());
        return fullLine.substring(0, start) + value + fullLine.substring(end);
    }

    static NormalizedCommand normalize(String input) {
        var leading = input.length() - input.stripLeading().length();
        var hasSlash = input.regionMatches(leading, "/", 0, 1);
        if (hasSlash) {
            return new NormalizedCommand(input.substring(leading + 1), leading + 1, true);
        }
        return new NormalizedCommand(input.substring(leading), leading, false);
    }

    static List<String> rootLocalFirst(String input, List<String> suggestions) {
        if (suggestions.size() < 2 || !isCompletingRoot(input)) {
            return suggestions;
        }
        var prefix = firstToken(normalize(input).command());
        if (prefix.contains(":")) {
            return suggestions;
        }
        var hasLocalCandidate = suggestions.stream()
                .map(FandConsoleCommandCompleter::rootToken)
                .anyMatch(root -> !root.contains(":") && root.startsWith(prefix));
        if (!hasLocalCandidate) {
            return suggestions;
        }
        return suggestions.stream()
                .filter(candidate -> !rootToken(candidate).contains(":"))
                .toList();
    }

    private static boolean isCompletingRoot(String input) {
        var command = normalize(input).command();
        return currentTokenStart(command) == 0;
    }

    private static int currentTokenStart(String command) {
        var trimmed = command.stripTrailing();
        if (trimmed.length() != command.length()) {
            return command.length();
        }
        var separator = Math.max(command.lastIndexOf(' '), command.lastIndexOf('\t'));
        return separator < 0 ? 0 : separator + 1;
    }

    private static String rootToken(String commandLine) {
        return firstToken(normalize(commandLine).command());
    }

    private static String firstToken(String command) {
        var trimmed = command.stripLeading();
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isWhitespace(trimmed.charAt(i))) {
                return trimmed.substring(0, i);
            }
        }
        return trimmed;
    }

    record NormalizedCommand(String command, int prefixLength, boolean hasSlash) {
    }
}
