package io.fand.api.command;

/**
 * Resolved command entry available for execution and completion.
 */
public interface RegisteredCommand {

    CommandDescriptor descriptor();

    CommandExecutor executor();

    CommandCompleter completer();
}
