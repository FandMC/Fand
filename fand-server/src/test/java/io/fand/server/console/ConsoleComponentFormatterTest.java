package io.fand.server.console;

import static org.assertj.core.api.Assertions.assertThat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class ConsoleComponentFormatterTest {
    @AfterEach
    void resetLanguage() {
        ConsoleLanguage.resetForTesting();
    }

    @Test
    void keepsPlainTextReadable() {
        assertThat(ConsoleComponentFormatter.ansi(Component.literal("plain"))).contains("plain");
    }

    @Test
    void normalizesConsoleLanguageNames() {
        assertThat(ConsoleLanguage.normalize("ZH-CN")).isEqualTo("zh_cn");
    }

    @Test
    void rendersTextColorAsAnsi() {
        String rendered = ConsoleComponentFormatter.ansi(Component.literal("ok").withStyle(ChatFormatting.GREEN));

        assertThat(rendered).contains("\u001B[38;2;85;255;85m");
        assertThat(rendered).contains("ok");
        assertThat(rendered).endsWith("\u001B[0m");
    }

    @Test
    void resetsBetweenSiblingStyles() {
        String rendered = ConsoleComponentFormatter.ansi(
                Component.literal("first").withStyle(ChatFormatting.RED)
                        .append(Component.literal("second").withStyle(ChatFormatting.YELLOW))
        );

        assertThat(rendered).contains("first\u001B[0m\u001B[38;2;255;255;85msecond");
    }

    @Test
    void localizesTranslatableTextForConsole() {
        ConsoleLanguage.useForTesting(
                "zh_cn",
                java.util.Map.of("commands.give.success.single", "已将 %s 个 %s 给予 %s")
        );

        String rendered = ConsoleComponentFormatter.ansi(Component.translatable(
                "commands.give.success.single",
                1,
                Component.literal("Stone").withStyle(ChatFormatting.GREEN),
                "Steve"
        ));

        assertThat(rendered).contains("已将 ");
        assertThat(rendered).contains("1");
        assertThat(rendered).contains("Stone");
        assertThat(rendered).contains("Steve");
        assertThat(rendered).contains("\u001B[38;2;85;255;85mStone");
    }
}
