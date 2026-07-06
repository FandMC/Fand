package io.fand.api.enchantment;

import com.google.gson.JsonObject;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public record CustomEnchantment(
        Key key,
        Component description,
        EnchantmentDefinition definition,
        EnchantmentEffects effects,
        java.util.List<io.fand.api.registry.RegistryReference> exclusiveSet
) {

    public CustomEnchantment(Key key, Component description, int maxLevel) {
        this(key, description, EnchantmentDefinition.allItems(maxLevel), EnchantmentEffects.emptyEffects(), java.util.List.of());
    }

    public CustomEnchantment(
            Key key,
            Component description,
            EnchantmentDefinition definition,
            JsonObject effects,
            java.util.List<io.fand.api.registry.RegistryReference> exclusiveSet) {
        this(key, description, definition, EnchantmentEffects.raw(effects), exclusiveSet);
    }

    public CustomEnchantment {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(effects, "effects");
        exclusiveSet = java.util.List.copyOf(exclusiveSet);
    }

    public int maxLevel() {
        return definition.maxLevel();
    }

    public CustomEnchantment withEffects(JsonObject effects) {
        return withEffects(EnchantmentEffects.raw(effects));
    }

    public CustomEnchantment withEffects(EnchantmentEffects effects) {
        return new CustomEnchantment(key, description, definition, effects, exclusiveSet);
    }

    public JsonObject effectsJson() {
        return effects.toJson();
    }

    public CustomEnchantment withDefinition(EnchantmentDefinition definition) {
        return new CustomEnchantment(key, description, definition, effects, exclusiveSet);
    }

    public CustomEnchantment withExclusiveSet(java.util.List<io.fand.api.registry.RegistryReference> exclusiveSet) {
        return new CustomEnchantment(key, description, definition, effects, exclusiveSet);
    }
}
