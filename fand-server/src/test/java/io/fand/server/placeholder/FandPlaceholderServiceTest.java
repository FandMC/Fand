package io.fand.server.placeholder;

import static org.assertj.core.api.Assertions.assertThat;

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
    void replacementProviderSupersedesOldRegistration() {
        var service = new FandPlaceholderService();
        var old = service.register("demo", (viewer, identifier) -> "old");

        service.register("demo", (viewer, identifier) -> "new");

        assertThat(old.active()).isFalse();
        assertThat(service.resolve(null, "demo_value")).contains("new");
    }
}
