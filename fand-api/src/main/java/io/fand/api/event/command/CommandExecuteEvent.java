package io.fand.api.event.command;

import io.fand.api.command.CommandSender;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread immediately before a command is executed.
 *
 * <p>The command text is exposed without a leading slash. Listeners may cancel
 * the event to suppress execution, or replace the command text before vanilla
 * or Fand command dispatch continues.
 */
public final class CommandExecuteEvent implements Event, Cancellable {

    private final CommandSender sender;
    private final String originalCommand;
    private String command;
    private boolean cancelled;

    public CommandExecuteEvent(CommandSender sender, String command) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.originalCommand = normalize(command);
        this.command = this.originalCommand;
    }

    public CommandSender sender() {
        return sender;
    }

    /** Command text as it was received, without a leading slash. */
    public String originalCommand() {
        return originalCommand;
    }

    /** Command text that will be executed unless the event is cancelled. */
    public String command() {
        return command;
    }

    public void setCommand(String command) {
        this.command = normalize(command);
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    private static String normalize(String command) {
        Objects.requireNonNull(command, "command");
        String stripped = command.stripLeading();
        return stripped.startsWith("/") ? stripped.substring(1) : stripped;
    }
}
