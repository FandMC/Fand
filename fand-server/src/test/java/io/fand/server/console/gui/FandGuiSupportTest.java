package io.fand.server.console.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class FandGuiSupportTest {

    @Test
    void themeButtonTextSyncsWhenThemeChangesExternally() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing components require a display");
        var service = new GuiThemeService(GuiTheme.DARK);
        var bar = FandGuiSupport.buildThemeBar(service);
        var button = findButton(bar.component());

        assertThat(button.getText()).isEqualTo("Dark");

        service.set(GuiTheme.LIGHT);
        flushEdt();
        assertThat(button.getText()).isEqualTo("Light");
    }

    @Test
    void cleanupDetachesTheButtonSyncListener() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing components require a display");
        var service = new GuiThemeService(GuiTheme.DARK);
        var bar = FandGuiSupport.buildThemeBar(service);
        var button = findButton(bar.component());

        bar.cleanup().run();
        service.set(GuiTheme.LIGHT);
        flushEdt();

        // Listener detached, so the button keeps its last rendered text.
        assertThat(button.getText()).isEqualTo("Dark");
    }

    private static JButton findButton(JComponent component) {
        for (var child : component.getComponents()) {
            if (child instanceof JButton button) {
                return button;
            }
        }
        throw new AssertionError("theme bar has no button");
    }

    private static void flushEdt() throws Exception {
        var ref = new AtomicReference<Boolean>();
        SwingUtilities.invokeAndWait(() -> ref.set(Boolean.TRUE));
        assertThat(ref.get()).isTrue();
    }
}
