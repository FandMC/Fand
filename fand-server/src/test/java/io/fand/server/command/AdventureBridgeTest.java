package io.fand.server.command;

import static org.assertj.core.api.Assertions.assertThat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AdventureBridgeTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void preservesRunCommandClickEvent() {
        assertClickEvent(ClickEvent.runCommand("/fand test"));
    }

    @Test
    void preservesSuggestCommandClickEvent() {
        assertClickEvent(ClickEvent.suggestCommand("/fand "));
    }

    @Test
    void preservesOpenUrlClickEvent() {
        assertClickEvent(ClickEvent.openUrl("https://fand.io/"));
    }

    @Test
    void preservesCopyToClipboardClickEvent() {
        assertClickEvent(ClickEvent.copyToClipboard("Fand"));
    }

    @Test
    void preservesChangePageClickEvent() {
        assertClickEvent(ClickEvent.changePage(2));
    }

    private static void assertClickEvent(ClickEvent clickEvent) {
        var original = Component.text("click").clickEvent(clickEvent);

        var vanilla = AdventureBridge.toVanilla(original, null);

        assertThat(vanilla.getStyle().getClickEvent()).isNotNull();
        assertThat(AdventureBridge.fromVanilla(vanilla, null)).isEqualTo(original);
    }
}
