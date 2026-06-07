package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread when a player submits a slash command, before
 * Fand or vanilla command dispatch starts.
 *
 * <p>The command text is exposed without a leading slash. Listeners may cancel
 * the event or replace the command text. For all command sources, including
 * console and command blocks, use
 * {@link io.fand.api.event.command.CommandExecuteEvent}.
 */
public final class PlayerCommandPreprocessEvent implements Event, Cancellable {

    private final Player player;
    private final String originalCommand;
    private String command;
    private boolean cancelled;

    public PlayerCommandPreprocessEvent(Player player, String command) {
        this.player = Objects.requireNonNull(player, "player");
        this.originalCommand = normalize(command);
        this.command = this.originalCommand;
    }

    public Player player() {
        return player;
    }

    /** Command text as the player sent it, without a leading slash. */
    public String originalCommand() {
        return originalCommand;
    }

    /** Command text that will be dispatched unless the event is cancelled. */
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
