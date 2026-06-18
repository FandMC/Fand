package io.fand.server.console.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class GuiThemeServiceTest {

    @Test
    void cycleAdvancesAndNotifiesListeners() {
        var service = new GuiThemeService(GuiTheme.DARK);
        var notifications = new AtomicInteger();
        service.addListener(notifications::incrementAndGet);

        assertThat(service.cycle()).isEqualTo(GuiTheme.LIGHT);
        assertThat(service.current()).isEqualTo(GuiTheme.LIGHT);
        assertThat(notifications).hasValue(1);
    }

    @Test
    void cyclePublishesUserSelection() {
        var selected = new AtomicReference<GuiTheme>();
        var service = new GuiThemeService(GuiTheme.DARK, selected::set);

        service.cycle();

        assertThat(selected).hasValue(GuiTheme.LIGHT);
    }

    @Test
    void setDoesNotPublishUserSelection() {
        var selected = new AtomicReference<GuiTheme>();
        var service = new GuiThemeService(GuiTheme.DARK, selected::set);

        service.set(GuiTheme.LIGHT);

        assertThat(selected.get()).isNull();
    }

    @Test
    void removedListenerStopsReceivingUpdates() throws Exception {
        var service = new GuiThemeService(GuiTheme.DARK);
        var notifications = new AtomicInteger();
        var handle = service.addListener(notifications::incrementAndGet);

        service.set(GuiTheme.LIGHT);
        handle.close();
        service.set(GuiTheme.SYSTEM);

        assertThat(notifications).hasValue(1);
    }

    @Test
    void closeDropsListenersAndIgnoresFurtherChanges() {
        var service = new GuiThemeService(GuiTheme.DARK);
        var notifications = new AtomicInteger();
        service.addListener(notifications::incrementAndGet);

        service.close();
        service.set(GuiTheme.LIGHT);

        assertThat(notifications).hasValue(0);
        assertThat(service.current()).isEqualTo(GuiTheme.DARK);
    }

    @Test
    void graphLineColorScalesWithBarHeightAndClamps() {
        var service = new GuiThemeService(GuiTheme.DARK);
        var dim = service.graphLineColor(0);
        var bright = service.graphLineColor(100);

        assertThat(brightness(bright)).isGreaterThan(brightness(dim));
        assertThat(service.graphLineColor(-50)).isEqualTo(dim);
        assertThat(service.graphLineColor(150)).isEqualTo(bright);
    }

    private static int brightness(java.awt.Color color) {
        return color.getRed() + color.getGreen() + color.getBlue();
    }
}
