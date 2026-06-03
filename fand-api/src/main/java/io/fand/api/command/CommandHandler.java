package io.fand.api.command;

import java.util.List;

/**
 * Handler for a registered command. The dispatcher tokenises raw input and
 * passes the remaining arguments here verbatim; argument parsing belongs to the
 * implementation.
 */
@FunctionalInterface
public interface CommandHandler {

    void execute(CommandSender sender, String label, List<String> args) throws Exception;

    /** Optional tab-completion hook. Returns suggestions for the final argument. */
    default List<String> suggest(CommandSender sender, String label, List<String> args) {
        return List.of();
    }
}
