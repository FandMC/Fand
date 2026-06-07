package io.fand.server.console.gui;

import java.awt.Component;
import java.awt.Container;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Applies a {@link GuiPalette} across a Swing component tree.
 *
 * <p>Kept separate from the patched vanilla GUI classes so the patch sites only
 * need a single call, and so the styling rules live in normal Fand source that
 * is unit-testable and not tangled with decompiled code. Call on the event
 * dispatch thread.
 */
public final class GuiStyles {

    private GuiStyles() {
    }

    /** Recolours {@code root} and every descendant for {@code palette}. */
    public static void apply(Component root, GuiPalette palette) {
        style(root, palette);
        if (root instanceof Container container) {
            for (var child : container.getComponents()) {
                apply(child, palette);
            }
        }
    }

    private static void style(Component component, GuiPalette palette) {
        switch (component) {
            case JButton button -> {
                // The platform look-and-feel (notably Windows) ignores setBackground
                // on buttons, so force a basic UI to make the themed colours stick.
                button.setUI(new BasicButtonUI());
                button.setOpaque(true);
                button.setBackground(palette.buttonBackground());
                button.setForeground(palette.foreground());
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(palette.border()),
                        BorderFactory.createEmptyBorder(3, 12, 3, 12)));
                button.setFocusPainted(false);
            }
            case JTextField field -> {
                field.setBackground(palette.inputBackground());
                field.setForeground(palette.inputForeground());
                field.setCaretColor(palette.foreground());
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(palette.border()),
                        BorderFactory.createEmptyBorder(3, 5, 3, 5)));
            }
            case JTextArea area -> {
                area.setBackground(palette.contentBackground());
                area.setForeground(palette.foreground());
                area.setCaretColor(palette.foreground());
            }
            case JList<?> list -> {
                list.setBackground(palette.contentBackground());
                list.setForeground(palette.foreground());
                list.setSelectionBackground(palette.selection());
                list.setSelectionForeground(palette.foreground());
            }
            case JScrollPane scrollPane -> {
                scrollPane.setBackground(palette.contentBackground());
                scrollPane.getViewport().setBackground(palette.contentBackground());
                retintTitledBorder(scrollPane, palette);
            }
            case JLabel label -> {
                label.setForeground(palette.foreground());
            }
            case JComponent generic -> {
                generic.setBackground(palette.windowBackground());
                generic.setForeground(palette.foreground());
                retintTitledBorder(generic, palette);
            }
            default -> {
                component.setBackground(palette.windowBackground());
                component.setForeground(palette.foreground());
            }
        }
    }

    private static void retintTitledBorder(JComponent component, GuiPalette palette) {
        if (component.getBorder() instanceof TitledBorder titled) {
            titled.setTitleColor(palette.mutedForeground());
        }
    }
}
