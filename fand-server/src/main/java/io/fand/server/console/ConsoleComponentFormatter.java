package io.fand.server.console;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class ConsoleComponentFormatter {

    private static final String RESET = "\u001B[0m";

    private ConsoleComponentFormatter() {
    }

    public static String ansi(Component message) {
        StringBuilder builder = new StringBuilder();
        appendComponent(builder, message, Style.EMPTY);
        return builder.toString();
    }

    private static void appendComponent(StringBuilder builder, Component component, Style parentStyle) {
        var style = component.getStyle().applyTo(parentStyle);
        appendContents(builder, component.getContents(), style);
        for (var sibling : component.getSiblings()) {
            appendComponent(builder, sibling, style);
        }
    }

    private static void appendContents(StringBuilder builder, ComponentContents contents, Style style) {
        if (contents instanceof TranslatableContents translatable) {
            appendTranslatable(builder, translatable, style);
            return;
        }

        contents.visit((ignored, text) -> {
            appendText(builder, style, text);
            return Optional.empty();
        }, style);
    }

    private static void appendTranslatable(StringBuilder builder, TranslatableContents translatable, Style style) {
        ConsoleLanguage.current()
                .format(translatable)
                .visit((partStyle, text) -> {
                    appendText(builder, partStyle.applyTo(style), text);
                    return Optional.empty();
                }, Style.EMPTY);
    }

    private static void appendText(StringBuilder builder, Style style, String text) {
        if (text.isEmpty()) {
            return;
        }
        appendStyle(builder, style);
        builder.append(text);
        builder.append(RESET);
    }

    private static void appendStyle(StringBuilder builder, Style style) {
        TextColor color = style.getColor();
        if (color != null) {
            int rgb = color.getValue();
            builder.append("\u001B[38;2;")
                    .append((rgb >> 16) & 0xFF)
                    .append(';')
                    .append((rgb >> 8) & 0xFF)
                    .append(';')
                    .append(rgb & 0xFF)
                    .append('m');
        }
        if (style.isBold()) {
            builder.append("\u001B[1m");
        }
        if (style.isItalic()) {
            builder.append("\u001B[3m");
        }
        if (style.isUnderlined()) {
            builder.append("\u001B[4m");
        }
        if (style.isStrikethrough()) {
            builder.append("\u001B[9m");
        }
    }
}
