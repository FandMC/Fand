package io.fand.api.enchantment;

import com.google.gson.JsonObject;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

public record EnchantmentView(
        Key key,
        @Nullable Component description,
        int maxLevel,
        @Nullable EnchantmentDefinition definition,
        JsonObject effects,
        List<io.fand.api.registry.RegistryReference> exclusiveSet
) {
    public EnchantmentView(Key key, @Nullable Component description, int maxLevel) {
        this(key, description, maxLevel, null, new JsonObject(), List.of());
    }

    public EnchantmentView {
        effects = effects == null ? new JsonObject() : effects.deepCopy();
        exclusiveSet = exclusiveSet == null ? List.of() : List.copyOf(exclusiveSet);
    }
}
