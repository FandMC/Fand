package io.fand.server.console.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Entry point the patched vanilla server GUI calls into for theming.
 *
 * <p>Concentrating the logic here keeps the vanilla patch to a handful of calls
 * and keeps the styling rules in normal, testable Fand source rather than in
 * decompiled code. The theme state itself is owned by the runtime
 * ({@link GuiThemeService}, reached via {@code FandHooks.guiThemes()}).
 *
 * <p>Threading is handled internally: every method that touches Swing marshals
 * onto the event dispatch thread itself, so callers (including the vanilla
 * {@code showFrameFor} path, which is not guaranteed to run on the EDT) do not
 * have to reason about it. {@link #palette()} and {@link #graphLineColor} only
 * read immutable theme data and are safe from any thread.
 */
public final class FandGuiSupport {

    private FandGuiSupport() {
    }

    private static GuiThemeService themes() {
        return io.fand.server.hooks.FandHooks.guiThemes();
    }

    /**
     * Builds the theme toggle bar shown at the top of the GUI. The button cycles
     * Dark → Light → System on click and stays in sync with the active theme even
     * when it changes elsewhere (e.g. via {@code /fand reload}), through a service
     * listener. Must be called on the EDT, like the rest of GUI construction in
     * {@code MinecraftServerGui}.
     *
     * @return the bar component plus a cleanup that releases the sync listener;
     *     the caller must register the cleanup as a GUI finalizer
     */
    public static ThemeBar buildThemeBar() {
        return buildThemeBar(themes());
    }

    /** Service-injecting variant of {@link #buildThemeBar()}, for testing. */
    static ThemeBar buildThemeBar(GuiThemeService themes) {
        var bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        bar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        var label = new JLabel("Theme:");
        var button = new JButton(themes.current().displayName());
        button.setFocusable(false);
        button.addActionListener(event -> themes.cycle());

        AutoCloseable handle = themes.addListener(
                () -> runOnEdt(() -> button.setText(themes.current().displayName())));

        bar.add(label);
        bar.add(button);
        return new ThemeBar(bar, releasing(handle));
    }

    /** A theme bar component paired with the cleanup that detaches its listener. */
    public record ThemeBar(JComponent component, Runnable cleanup) {
    }

    /**
     * Applies the active palette to {@code root}'s window and registers a listener
     * that restyles and repaints whenever the theme changes. Theme-change
     * notifications may arrive on any thread, so the listener marshals onto the
     * EDT before touching Swing.
     *
     * @return a cleanup action that detaches the listener; register it as a GUI finalizer
     */
    public static Runnable attachTheming(Component root) {
        runOnEdt(() -> applyTheme(root));
        AutoCloseable handle = themes().addListener(() -> runOnEdt(() -> applyTheme(root)));
        return releasing(handle);
    }

    /** Wraps a listener handle as an idempotent, exception-swallowing finalizer. */
    private static Runnable releasing(AutoCloseable handle) {
        return () -> {
            try {
                handle.close();
            } catch (Exception ignored) {
                // Listener removal cannot fail; ignore to keep finalizers quiet.
            }
        };
    }

    /** Recolours {@code root}'s window for the active theme, on the EDT. */
    public static void applyTheme(Component root) {
        runOnEdt(() -> {
            var target = targetFor(root);
            GuiStyles.apply(target, themes().palette());
            target.invalidate();
            target.validate();
            target.repaint();
        });
    }

    private static Component targetFor(Component root) {
        Window window = SwingUtilities.getWindowAncestor(root);
        return window != null ? window : root;
    }

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    /** Current palette. Reads immutable theme data; safe from any thread. */
    public static GuiPalette palette() {
        return themes().palette();
    }

    /** Graph-bar colour for the given height. Safe from any thread. */
    public static Color graphLineColor(int barHeight) {
        return themes().graphLineColor(barHeight);
    }
}
