package io.fand.api.command;

import java.util.List;

/**
 * Provides command suggestions through the structured command context.
 */
@FunctionalInterface
public interface CommandSuggestionProvider {

    List<String> suggest(CommandContext context) throws Exception;
}
