package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread before a player updates sign text.
 */
public final class SignChangeEvent implements Event, Cancellable {

    private final Player player;
    private final Block block;
    private final boolean frontText;
    private final List<String> lines;
    private boolean cancelled;

    public SignChangeEvent(Player player, Block block, boolean frontText, List<String> lines) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.frontText = frontText;
        this.lines = new ArrayList<>(Objects.requireNonNull(lines, "lines"));
    }

    public Player player() {
        return player;
    }

    public Block block() {
        return block;
    }

    public boolean frontText() {
        return frontText;
    }

    /**
     * Live mutable plain-text sign lines. Vanilla already filtered formatting
     * before this event is fired.
     */
    public List<String> lines() {
        return lines;
    }

    public void setLine(int index, String line) {
        lines.set(index, Objects.requireNonNull(line, "line"));
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
