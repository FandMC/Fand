package io.fand.server.enchantment;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.enchantment.CustomEnchantment;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class FandEnchantmentRegistryTest {

    private static final Key KEY = Key.key("demo:quickening");

    @Test
    void registersCustomEnchantmentWithoutAttachedServer() {
        var registry = new FandEnchantmentRegistry(() -> null);
        var registration = registry.register(new CustomEnchantment(KEY, Component.text("Quickening"), 3));

        assertThat(registration.key()).isEqualTo(KEY);
        assertThat(registration.active()).isTrue();
        assertThat(registry.enchantment(KEY)).get().satisfies(view -> {
            assertThat(view.key()).isEqualTo(KEY);
            assertThat(view.description()).isEqualTo(Component.text("Quickening"));
            assertThat(view.maxLevel()).isEqualTo(3);
        });

        registration.close();

        assertThat(registration.active()).isFalse();
        assertThat(registry.enchantment(KEY)).isEmpty();
    }

    @Test
    void oldRegistrationCannotRemoveReplacement() {
        var registry = new FandEnchantmentRegistry(() -> null);
        var first = registry.register(new CustomEnchantment(KEY, Component.text("First"), 1));
        var second = registry.register(new CustomEnchantment(KEY, Component.text("Second"), 5));

        first.close();

        assertThat(first.active()).isFalse();
        assertThat(second.active()).isTrue();
        assertThat(registry.enchantment(KEY)).get()
                .extracting(view -> view.description())
                .isEqualTo(Component.text("Second"));
    }
}
