package io.fand.api.command;

import java.util.List;

/**
 * Provides suggestions for a command invocation.
 */
@FunctionalInterface
public interface CommandCompleter {

    List<String> complete(CommandSender sender, String label, List<String> args) throws Exception;
}
