package io.fand.server.console.gui;

import java.awt.Color;

/**
 * Resolved colours for a single {@link GuiTheme}.
 *
 * @param windowBackground frame and outer panel fill
 * @param contentBackground log area and list backgrounds
 * @param inputBackground command field background
 * @param buttonBackground theme toggle button fill
 * @param foreground primary text
 * @param mutedForeground secondary text (border titles, hints)
 * @param inputForeground command field text
 * @param border component borders and separators
 * @param accent focus ring and links
 * @param graphBackground performance graph fill
 * @param graphLine performance graph line at full intensity
 * @param selection list selection background
 */
public record GuiPalette(
        Color windowBackground,
        Color contentBackground,
        Color inputBackground,
        Color buttonBackground,
        Color foreground,
        Color mutedForeground,
        Color inputForeground,
        Color border,
        Color accent,
        Color graphBackground,
        Color graphLine,
        Color selection) {
}
