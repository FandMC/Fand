package io.fand.server.console.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.server.config.ConfigException;
import org.junit.jupiter.api.Test;

class GuiThemeTest {

    @Test
    void parsesKnownThemeNames() {
        assertThat(GuiTheme.fromConfig("dark")).isEqualTo(GuiTheme.DARK);
        assertThat(GuiTheme.fromConfig("LIGHT")).isEqualTo(GuiTheme.LIGHT);
        assertThat(GuiTheme.fromConfig(" System ")).isEqualTo(GuiTheme.SYSTEM);
    }

    @Test
    void defaultsToSystemForBlankOrNull() {
        assertThat(GuiTheme.fromConfig(null)).isEqualTo(GuiTheme.SYSTEM);
        assertThat(GuiTheme.fromConfig("")).isEqualTo(GuiTheme.SYSTEM);
        assertThat(GuiTheme.fromConfig("   ")).isEqualTo(GuiTheme.SYSTEM);
    }

    @Test
    void rejectsUnknownThemeNames() {
        assertThatThrownBy(() -> GuiTheme.fromConfig("solarized"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("console.gui.theme");
    }

    @Test
    void cycleVisitsEveryThemeAndReturns() {
        assertThat(GuiTheme.DARK.next()).isEqualTo(GuiTheme.LIGHT);
        assertThat(GuiTheme.LIGHT.next()).isEqualTo(GuiTheme.SYSTEM);
        assertThat(GuiTheme.SYSTEM.next()).isEqualTo(GuiTheme.DARK);
    }

    @Test
    void concreteThemesResolveToThemselves() {
        assertThat(GuiTheme.DARK.resolved()).isEqualTo(GuiTheme.DARK);
        assertThat(GuiTheme.LIGHT.resolved()).isEqualTo(GuiTheme.LIGHT);
    }

    @Test
    void systemResolvesToConcreteThemeWithPalette() {
        assertThat(GuiTheme.SYSTEM.resolved()).isIn(GuiTheme.DARK, GuiTheme.LIGHT);
        assertThat(GuiTheme.SYSTEM.palette()).isNotNull();
    }

    @Test
    void everyThemeExposesAFullPalette() {
        for (var theme : GuiTheme.values()) {
            var palette = theme.palette();
            assertThat(palette.windowBackground()).as("%s window", theme).isNotNull();
            assertThat(palette.foreground()).as("%s foreground", theme).isNotNull();
            assertThat(palette.accent()).as("%s accent", theme).isNotNull();
            assertThat(palette.buttonBackground()).as("%s button", theme).isNotNull();
            assertThat(palette.graphLine()).as("%s graphLine", theme).isNotNull();
        }
    }
}
