package io.fand.server.text;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.server.placeholder.FandPlaceholderService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

final class FandMiniMessageServiceTest {

    @Test
    void parsesRgbAndGradientTags() {
        var service = new FandMiniMessageService(new FandPlaceholderService());

        var json = GsonComponentSerializer.gson().serialize(
                service.parse("<#ff0000>Red</#ff0000> <gradient:#00ff00:#0000ff>Go</gradient>"))
                .toLowerCase(java.util.Locale.ROOT);

        assertThat(json)
                .contains("red")
                .contains("#ff0000")
                .contains("#00ff00")
                .contains("#0000ff");
    }

    @Test
    void replacesFandPlaceholdersBeforeParsingMiniMessage() {
        var placeholders = new FandPlaceholderService();
        placeholders.register("demo", (viewer, identifier) -> "<#00ff00>Fand</#00ff00>");
        var service = new FandMiniMessageService(placeholders);

        var json = GsonComponentSerializer.gson().serialize(service.parse("Hello %demo_name%"))
                .toLowerCase(java.util.Locale.ROOT);

        assertThat(json)
                .contains("hello ")
                .contains("fand")
                .contains("#00ff00");
    }

    @Test
    void acceptsCustomAdventureTagResolvers() {
        var service = new FandMiniMessageService(new FandPlaceholderService());

        var parsed = service.parse(
                "Hello <subject>",
                TagResolver.resolver("subject", Tag.inserting(Component.text("Alex"))));

        assertThat(GsonComponentSerializer.gson().serialize(parsed)).contains("Hello ", "Alex");
    }
}
