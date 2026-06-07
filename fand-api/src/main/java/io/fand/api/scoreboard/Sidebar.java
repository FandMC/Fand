package io.fand.api.scoreboard;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;

/**
 * Per-player sidebar display shown in the vanilla scoreboard area.
 *
 * <p>This is intentionally a small display API, not a full scoreboard/team
 * model. Lines are rendered from top to bottom and the numeric score column is
 * hidden by the server bridge.
 */
public record Sidebar(Component title, List<Line> lines) {

    /** Vanilla sidebars show at most fifteen rows. */
    public static final int MAX_LINES = 15;

    public Sidebar {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
        if (lines.size() > MAX_LINES) {
            throw new IllegalArgumentException("Sidebar cannot contain more than " + MAX_LINES + " lines");
        }
    }

    public Sidebar(Component title, Component... lines) {
        this(title, List.of(lines).stream().map(Line::new).toList());
    }

    public static Sidebar of(Component title, List<Component> lines) {
        return new Sidebar(title, lines.stream().map(Line::new).toList());
    }

    public static Sidebar of(Component title, Component... lines) {
        return new Sidebar(title, lines);
    }

    /** One sidebar row. */
    public record Line(Component text) {

        public Line {
            Objects.requireNonNull(text, "text");
        }
    }
}
