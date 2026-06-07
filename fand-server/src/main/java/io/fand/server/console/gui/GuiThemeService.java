package io.fand.server.console.gui;

import java.awt.Color;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runtime-owned holder for the active {@link GuiTheme} and the listeners that
 * repaint the server GUI when it changes.
 *
 * <p>Owned by {@code FandServer}: constructed from config during bootstrap and
 * {@link #close() closed} on shutdown, which drops every listener so the GUI can
 * be torn down without leaking references. Instances are independent, so tests
 * can exercise theming without touching global state.
 *
 * <p>Threading: {@code current} is {@code volatile} and the listener list is
 * copy-on-write, so {@link #current()}, {@link #palette()}, {@link #set} and
 * {@link #cycle()} are safe to call from any thread. Listeners run on the thread
 * that calls {@link #set}/{@link #cycle()}; a listener that touches Swing is
 * responsible for marshalling onto the event dispatch thread (the GUI listener
 * registered by {@link FandGuiSupport#attachTheming} does exactly that).
 */
public final class GuiThemeService implements AutoCloseable {

    private volatile GuiTheme current;
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean closed;

    public GuiThemeService(GuiTheme initial) {
        this.current = Objects.requireNonNull(initial, "initial");
    }

    public GuiTheme current() {
        return current;
    }

    public GuiPalette palette() {
        return current.palette();
    }

    public void set(GuiTheme theme) {
        Objects.requireNonNull(theme, "theme");
        if (closed) {
            return;
        }
        current = theme;
        for (var listener : listeners) {
            listener.run();
        }
    }

    /** Advances to the next theme and notifies listeners. Returns the new theme. */
    public GuiTheme cycle() {
        var next = current.next();
        set(next);
        return next;
    }

    /**
     * Colour for a memory-usage graph bar of the given height (0-100), scaled from
     * a dim baseline up to the theme's full-intensity graph line so the plot stays
     * legible on both dark and light backgrounds.
     */
    public Color graphLineColor(int barHeight) {
        var line = current.palette().graphLine();
        int clamped = Math.max(0, Math.min(100, barHeight));
        float intensity = 0.45f + 0.55f * (clamped / 100.0f);
        return new Color(
                Math.round(line.getRed() * intensity),
                Math.round(line.getGreen() * intensity),
                Math.round(line.getBlue() * intensity));
    }

    /** Registers a listener and returns a handle that removes it when closed. */
    public AutoCloseable addListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public void close() {
        closed = true;
        listeners.clear();
    }
}
