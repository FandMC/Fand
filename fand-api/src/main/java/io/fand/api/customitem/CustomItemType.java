package io.fand.api.customitem;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Registered custom-item definition. The item is still a vanilla stack on the
 * wire; Fand stores the custom id in {@code minecraft:custom_data}.
 */
public record CustomItemType(Key id, ItemType baseType, ItemStack template) {

    public CustomItemType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(baseType, "baseType");
        Objects.requireNonNull(template, "template");
        if (template.isEmpty()) {
            throw new IllegalArgumentException("template must not be empty");
        }
        if (!baseType.equals(template.type())) {
            throw new IllegalArgumentException("template type must match baseType");
        }
    }

    public static CustomItemType of(Key id, ItemStack template) {
        Objects.requireNonNull(template, "template");
        if (template.isEmpty()) {
            throw new IllegalArgumentException("template must not be empty");
        }
        return new CustomItemType(id, template.type(), template);
    }
}
