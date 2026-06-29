package io.fand.server.console;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class ConsoleComponentFormatter {

    private static final String RESET = "\u001B[0m";

    private ConsoleComponentFormatter() {
    }

    public static String ansi(Component message) {
        StringBuilder builder = new StringBuilder();
        message.visit((style, text) -> {
            appendStyle(builder, style);
            builder.append(text);
            builder.append(RESET);
            return Optional.empty();
        }, Style.EMPTY);
        return builder.toString();
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
