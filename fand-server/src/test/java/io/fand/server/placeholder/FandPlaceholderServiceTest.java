package io.fand.server.placeholder;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.placeholder.PlaceholderContext;
import io.fand.api.placeholder.PlaceholderProvider;
import org.junit.jupiter.api.Test;

final class FandPlaceholderServiceTest {

    @Test
    void resolvesNamespacedPlaceholders() {
        var service = new FandPlaceholderService();
        service.register("demo", (viewer, identifier) -> switch (identifier) {
            case "demo_name" -> "Fand";
            case "demo_online" -> "3";
            default -> null;
        });

        assertThat(service.resolve(null, "demo_name")).contains("Fand");
        assertThat(service.replace(null, "Hello %demo_name%: %demo_online%/%missing_value%"))
                .isEqualTo("Hello Fand: 3/%missing_value%");
    }

    @Test
    void resolvesContextualPlaceholders() {
        var service = new FandPlaceholderService();
        service.register("rel", PlaceholderProvider.contextual((context, identifier) ->
                context.value("mode", String.class).map(mode -> identifier + ":" + mode).orElse(null)));
        var context = PlaceholderContext.builder().value("mode", "viewer-target").build();

        assertThat(service.resolve("rel_value", context)).contains("rel_value:viewer-target");
        assertThat(service.replace("%rel_value%", context)).isEqualTo("rel_value:viewer-target");
    }

    @Test
    void replacementProviderSupersedesOldRegistration() {
        var service = new FandPlaceholderService();
        var old = service.register("demo", (viewer, identifier) -> "old");

        service.register("demo", (viewer, identifier) -> "new");

        assertThat(old.active()).isFalse();
        assertThat(service.resolve(null, "demo_value")).contains("new");
    }
}
