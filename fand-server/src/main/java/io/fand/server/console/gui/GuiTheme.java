package io.fand.server.console.gui;

import io.fand.server.config.ConfigException;
import java.awt.Color;
import java.util.Locale;
import javax.swing.UIManager;

/**
 * Colour scheme for the Fand server GUI.
 *
 * <p>{@link #SYSTEM} resolves to {@link #DARK} or {@link #LIGHT} at paint time by
 * inspecting the luminance of the active look-and-feel's panel background, so a
 * machine running a dark desktop theme gets the dark palette without explicit
 * configuration.
 */
public enum GuiTheme {

    DARK(new GuiPalette(
            new Color(0x1E1F22),  // windowBackground
            new Color(0x2B2D31),  // contentBackground
            new Color(0x202225),  // inputBackground
            new Color(0x383A40),  // buttonBackground
            new Color(0xDCDDDE),  // foreground
            new Color(0x96989D),  // mutedForeground
            new Color(0xF2F3F5),  // inputForeground
            new Color(0x4F545C),  // border
            new Color(0x5865F2),  // accent
            new Color(0x16171A),  // graphBackground
            new Color(0x4ED784),  // graphLine
            new Color(0x3F4248))), // selection

    LIGHT(new GuiPalette(
            new Color(0xF2F3F5),  // windowBackground
            new Color(0xFFFFFF),  // contentBackground
            new Color(0xFFFFFF),  // inputBackground
            new Color(0xE3E5E8),  // buttonBackground
            new Color(0x2E3338),  // foreground
            new Color(0x6A7178),  // mutedForeground
            new Color(0x2E3338),  // inputForeground
            new Color(0xD4D7DC),  // border
            new Color(0x5865F2),  // accent
            new Color(0xFBFBFB),  // graphBackground
            new Color(0xB02525),  // graphLine
            new Color(0xCDDCF6))), // selection

    SYSTEM(LIGHT.palette);

    private final GuiPalette palette;

    GuiTheme(GuiPalette palette) {
        this.palette = palette;
    }

    /**
     * Returns the palette to paint with. {@link #SYSTEM} resolves to the dark or
     * light palette based on the current look-and-feel background luminance.
     */
    public GuiPalette palette() {
        if (this != SYSTEM) {
            return palette;
        }
        return resolveSystem().palette;
    }

    /** Resolved concrete theme, collapsing {@link #SYSTEM} to {@link #DARK}/{@link #LIGHT}. */
    public GuiTheme resolved() {
        return this == SYSTEM ? resolveSystem() : this;
    }

    /** The next theme in a DARK → LIGHT → SYSTEM cycle, used by the in-GUI toggle. */
    public GuiTheme next() {
        return switch (this) {
            case DARK -> LIGHT;
            case LIGHT -> SYSTEM;
            case SYSTEM -> DARK;
        };
    }

    public String displayName() {
        return switch (this) {
            case DARK -> "Dark";
            case LIGHT -> "Light";
            case SYSTEM -> "System";
        };
    }

    public String configValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses a configured theme name. Blank values default to {@link #SYSTEM};
     * unrecognised values are rejected with a {@link ConfigException} so a typo is
     * surfaced rather than silently swallowed, matching how other config enums are
     * validated.
     */
    public static GuiTheme fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return SYSTEM;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "dark" -> DARK;
            case "light" -> LIGHT;
            case "system" -> SYSTEM;
            default -> throw new ConfigException(
                    "console.gui.theme must be one of: dark, light, system");
        };
    }

    private static GuiTheme resolveSystem() {
        var background = UIManager.getColor("Panel.background");
        if (background == null) {
            return LIGHT;
        }
        double luminance = (0.299 * background.getRed()
                + 0.587 * background.getGreen()
                + 0.114 * background.getBlue()) / 255.0;
        return luminance < 0.5 ? DARK : LIGHT;
    }
}
