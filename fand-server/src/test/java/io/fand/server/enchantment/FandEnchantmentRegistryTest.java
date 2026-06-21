package io.fand.server.enchantment;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fand.api.enchantment.CustomEnchantment;
import io.fand.api.enchantment.EnchantmentCost;
import io.fand.api.enchantment.EnchantmentDefinition;
import io.fand.api.enchantment.EnchantmentEffects;
import io.fand.api.enchantment.EnchantmentLevelValue;
import io.fand.api.enchantment.EnchantmentSlotGroup;
import io.fand.api.enchantment.EnchantmentTarget;
import io.fand.api.enchantment.EnchantmentValueEffect;
import io.fand.api.registry.RegistryReference;
import java.util.List;
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

    @Test
    void customEnchantmentViewKeepsDefinitionEffectsAndExclusiveSetWithoutAttachedServer() {
        var effects = new JsonObject();
        effects.add("minecraft:post_attack", new JsonArray());
        var definition = EnchantmentDefinition.builder()
                .supportedItems(List.of(RegistryReference.tag(Key.key("minecraft:swords"))))
                .primaryItems(List.of(RegistryReference.key(Key.key("minecraft:diamond_sword"))))
                .weight(10)
                .maxLevel(4)
                .minCost(new EnchantmentCost(5, 8))
                .maxCost(new EnchantmentCost(25, 8))
                .anvilCost(3)
                .slots(List.of(EnchantmentSlotGroup.MAINHAND))
                .build();
        var enchantment = new CustomEnchantment(
                KEY,
                Component.text("Quickening"),
                definition,
                effects,
                List.of(RegistryReference.tag(Key.key("minecraft:exclusive_set/damage"))));

        var registry = new FandEnchantmentRegistry(() -> null);
        registry.register(enchantment);

        assertThat(registry.enchantment(KEY)).get().satisfies(view -> {
            assertThat(view.definition()).isEqualTo(definition);
            assertThat(view.effectsJson()).isEqualTo(effects);
            assertThat(view.exclusiveSet()).containsExactly(RegistryReference.tag(Key.key("minecraft:exclusive_set/damage")));
        });
    }

    @Test
    void typedEffectsBuildVanillaComponentJson() {
        var effects = EnchantmentEffects.builder()
                .damage(EnchantmentValueEffect.add(EnchantmentLevelValue.linear(2.0F, 1.0F)))
                .postAttack(
                        EnchantmentTarget.ATTACKER,
                        EnchantmentTarget.VICTIM,
                        io.fand.api.enchantment.EnchantmentEntityEffect.ignite(EnchantmentLevelValue.constant(4.0F)))
                .preventArmorChange()
                .build();
        var registry = new FandEnchantmentRegistry(() -> null);
        registry.register(new CustomEnchantment(KEY, Component.text("Typed"), 1).withEffects(effects));

        assertThat(registry.enchantment(KEY)).get().satisfies(view -> {
            assertThat(view.effects()).isEqualTo(effects);
            var json = view.effectsJson();
            assertThat(json.getAsJsonArray("minecraft:damage")).hasSize(1);
            assertThat(json.getAsJsonArray("minecraft:damage").get(0).getAsJsonObject()
                    .getAsJsonObject("effect").get("type").getAsString()).isEqualTo("minecraft:add");
            assertThat(json.getAsJsonArray("minecraft:post_attack")).hasSize(1);
            assertThat(json.getAsJsonObject("minecraft:prevent_armor_change")).isNotNull();
        });
    }
}
