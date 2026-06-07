package io.fand.server.console.gui;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.Nullable;

/**
 * Process-wide holder for the active {@link GuiTheme} and a registry of change
 * listeners that repaint the server GUI when the theme switches at runtime.
 *
 * <p>Threading: {@link #initFromConfig} runs once on the main thread during
 * runtime construction; every other access happens on the Swing event dispatch
 * thread. {@code current} is {@code volatile} and the listener list is
 * copy-on-write so a stray read from another thread stays consistent.
 */
public final class GuiThemes {

    private static volatile GuiTheme current = GuiTheme.SYSTEM;
    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    private GuiThemes() {
    }

    public static void initFromConfig(@Nullable String configuredTheme) {
        current = GuiTheme.fromConfig(configuredTheme);
    }

    public static GuiTheme current() {
        return current;
    }

    public static GuiPalette palette() {
        return current.palette();
    }

    public static void set(GuiTheme theme) {
        current = Objects.requireNonNull(theme, "theme");
        for (var listener : listeners) {
            listener.run();
        }
    }

    /** Advances to the next theme and notifies listeners. Returns the new theme. */
    public static GuiTheme cycle() {
        var next = current.next();
        set(next);
        return next;
    }

    /** Registers a listener and returns a handle that removes it when closed. */
    public static AutoCloseable addListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
