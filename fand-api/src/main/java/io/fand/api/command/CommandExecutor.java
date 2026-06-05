package io.fand.api.command;

import java.util.List;

/**
 * Executes a command invocation.
 */
@FunctionalInterface
public interface CommandExecutor {

    void execute(CommandSender sender, String label, List<String> args) throws Exception;
}
