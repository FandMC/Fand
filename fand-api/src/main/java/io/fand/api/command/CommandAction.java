package io.fand.api.command;

/**
 * Executes a command through the structured command context.
 */
@FunctionalInterface
public interface CommandAction {

    void execute(CommandContext context) throws Exception;
}
