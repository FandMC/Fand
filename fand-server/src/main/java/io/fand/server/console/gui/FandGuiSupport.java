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
 * decompiled code. All methods must be called on the Swing event dispatch
 * thread.
 */
public final class FandGuiSupport {

    private FandGuiSupport() {
    }

    /**
     * Builds the theme toggle bar shown at the top of the GUI. The button cycles
     * Dark → Light → System and reflects the active theme.
     */
    public static JComponent buildThemeBar() {
        var bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        bar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        var label = new JLabel("Theme:");
        var button = new JButton(GuiThemes.current().displayName());
        button.setFocusable(false);
        button.addActionListener(event -> button.setText(GuiThemes.cycle().displayName()));

        bar.add(label);
        bar.add(button);
        return bar;
    }

    /**
     * Applies the active palette to the whole window and registers a listener that
     * restyles and repaints whenever the theme changes at runtime.
     *
     * @return a cleanup action that detaches the listener; register it as a GUI finalizer
     */
    public static Runnable attachTheming(Component root) {
        applyTheme(root);
        AutoCloseable handle = GuiThemes.addListener(() -> SwingUtilities.invokeLater(() -> {
            applyTheme(root);
            repaintWindow(root);
        }));
        return () -> {
            try {
                handle.close();
            } catch (Exception ignored) {
                // Listener removal cannot fail; ignore to keep finalizers quiet.
            }
        };
    }

    /** Recolours {@code root} (and its window, if any) for the active theme. */
    public static void applyTheme(Component root) {
        var target = targetFor(root);
        GuiStyles.apply(target);
        target.invalidate();
        target.validate();
        target.repaint();
    }

    private static Component targetFor(Component root) {
        Window window = SwingUtilities.getWindowAncestor(root);
        return window != null ? window : root;
    }

    private static void repaintWindow(Component root) {
        targetFor(root).repaint();
    }

    /** Current palette, exposed so patched paint code can read theme colours. */
    public static GuiPalette palette() {
        return GuiThemes.palette();
    }

    /**
     * Colour for a memory-usage graph bar of the given height (0-100), scaled
     * from a dim baseline up to the theme's full-intensity graph line so the plot
     * stays legible on both dark and light backgrounds.
     */
    public static Color graphLineColor(int barHeight) {
        var line = palette().graphLine();
        int clamped = Math.max(0, Math.min(100, barHeight));
        float intensity = 0.45f + 0.55f * (clamped / 100.0f);
        int r = Math.round(line.getRed() * intensity);
        int gg = Math.round(line.getGreen() * intensity);
        int b = Math.round(line.getBlue() * intensity);
        return new Color(r, gg, b);
    }
}
