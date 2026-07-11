package io.fand.api.command;

import java.util.List;

/**
 * Provides command suggestions through the structured command context. During
 * completion, {@link CommandContext#args()} includes the current incomplete
 * argument while parsed arguments contain only completed values.
 */
@FunctionalInterface
public interface CommandSuggestionProvider {

    List<String> suggest(CommandContext context) throws Exception;
}
