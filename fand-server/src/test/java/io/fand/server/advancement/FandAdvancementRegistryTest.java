package io.fand.server.advancement;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.advancement.CustomAdvancement;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class FandAdvancementRegistryTest {

    private static final Key KEY = Key.key("demo:first_step");

    @Test
    void registersCustomAdvancementWithoutAttachedServer() {
        var registry = new FandAdvancementRegistry(() -> null);
        var registration = registry.register(new CustomAdvancement(
                KEY,
                Component.text("First Step"),
                Component.text("Do the thing"),
                List.of("done")));

        assertThat(registration.key()).isEqualTo(KEY);
        assertThat(registration.active()).isTrue();
        assertThat(registry.advancement(KEY)).get().satisfies(view -> {
            assertThat(view.key()).isEqualTo(KEY);
            assertThat(view.title()).isEqualTo(Component.text("First Step"));
            assertThat(view.description()).isEqualTo(Component.text("Do the thing"));
        });

        registration.close();

        assertThat(registration.active()).isFalse();
        assertThat(registry.advancement(KEY)).isEmpty();
    }

    @Test
    void oldRegistrationCannotRemoveReplacement() {
        var registry = new FandAdvancementRegistry(() -> null);
        var first = registry.register(new CustomAdvancement(KEY, Component.text("First"), Component.text("One"), List.of("done")));
        var second = registry.register(new CustomAdvancement(KEY, Component.text("Second"), Component.text("Two"), List.of("done")));

        first.close();

        assertThat(first.active()).isFalse();
        assertThat(second.active()).isTrue();
        assertThat(registry.advancement(KEY)).get()
                .extracting(view -> view.title())
                .isEqualTo(Component.text("Second"));
    }
}
