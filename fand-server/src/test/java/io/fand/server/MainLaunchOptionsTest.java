package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.server.console.ConsoleLanguage;
import org.junit.jupiter.api.Test;

final class MainLaunchOptionsTest {
    @Test
    void consumesInlineLanguageOptionBeforeVanillaLaunch() {
        var options = Main.LaunchOptions.parse(new String[] {"nogui", "-lang=zh_cn", "--port", "25565"});

        assertThat(options.consoleLanguage()).isEqualTo("zh_cn");
        assertThat(options.minecraftArgs()).containsExactly("nogui", "--port", "25565");
    }

    @Test
    void consumesSeparateLanguageOptionBeforeVanillaLaunch() {
        var options = Main.LaunchOptions.parse(new String[] {"--lang", "zh_cn", "nogui"});

        assertThat(options.consoleLanguage()).isEqualTo("zh_cn");
        assertThat(options.minecraftArgs()).containsExactly("nogui");
    }

    @Test
    void consumesLanguageFlagWithoutValue() {
        var options = Main.LaunchOptions.parse(new String[] {"--lang", "nogui"});

        assertThat(options.consoleLanguage()).isEqualTo(ConsoleLanguage.DEFAULT_LOCALE);
        assertThat(options.minecraftArgs()).containsExactly("nogui");
    }

    @Test
    void consumesSeparatedLanguageOnlyWhenValueLooksLikeLocale() {
        var options = Main.LaunchOptions.parse(new String[] {"--lang", "zh-cn", "nogui"});

        assertThat(options.consoleLanguage()).isEqualTo("zh-cn");
        assertThat(options.minecraftArgs()).containsExactly("nogui");
    }
}
