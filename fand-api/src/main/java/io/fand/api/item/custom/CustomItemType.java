package io.fand.api.item.custom;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemTagKey;
import io.fand.api.item.ItemType;
import io.fand.api.item.component.ItemComponentKeys;
import io.fand.api.item.component.ItemComponents;
import io.fand.api.item.component.ItemTool;
import io.fand.api.block.custom.CustomBlockType;
import io.fand.api.tag.Tag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Logical custom-item type backed by a vanilla item type and a default component patch.
 *
 * <p>The caller may select any vanilla {@link #baseType()}; Fand does not restrict custom
 * items to a fixed carrier list. The custom key is exposed through {@link #key()} and
 * survives inventory and event round trips. On the wire, Fand encodes the stack as the
 * selected base plus components, so unmodified clients do not need a matching registry entry.
 */
public record CustomItemType(
        Key id,
        ItemType baseType,
        ItemComponents defaultComponents,
        List<ItemTool.Rule> customBlockToolRules
) implements ItemType {

    public CustomItemType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(baseType, "baseType");
        defaultComponents = defaultComponents == null ? ItemComponents.EMPTY : defaultComponents;
        customBlockToolRules = List.copyOf(Objects.requireNonNull(customBlockToolRules, "customBlockToolRules"));
        if (baseType instanceof CustomItemType) {
            throw new IllegalArgumentException("baseType must be a vanilla item type");
        }
        if (!defaultComponents.touchedKeys().contains(ItemComponentKeys.ITEM_MODEL)) {
            defaultComponents = defaultComponents.withKey(ItemComponentKeys.ITEM_MODEL, id);
        }
        new ItemStack(baseType, 1, defaultComponents);
    }

    public CustomItemType(Key id, ItemType baseType, ItemComponents defaultComponents) {
        this(id, baseType, defaultComponents, List.of());
    }

    public CustomItemType(Key id, ItemType baseType) {
        this(id, baseType, ItemComponents.EMPTY);
    }

    public static CustomItemType of(Key id, ItemType baseType) {
        return new CustomItemType(id, baseType);
    }

    public static CustomItemType of(Key id, ItemType baseType, ItemComponents defaultComponents) {
        return new CustomItemType(id, baseType, defaultComponents);
    }

    public static CustomItemType of(Key id, ItemStack template) {
        Objects.requireNonNull(template, "template");
        if (template.empty()) {
            throw new IllegalArgumentException("template must not be empty");
        }
        return new CustomItemType(id, template.type(), template.components());
    }

    public static Builder builder(Key id, ItemType baseType) {
        return new Builder(id, baseType);
    }

    /** Vanilla representation before Fand adds the internal custom identity. */
    public ItemStack template() {
        return new ItemStack(baseType, 1, defaultComponents);
    }

    @Override
    public Key key() {
        return id;
    }

    @Override
    public int maxStackSize() {
        return template().maxStackSize();
    }

    @Override
    public ItemStack stack(int amount) {
        return new ItemStack(this, amount);
    }

    @Override
    public ItemStack stack(int amount, ItemComponents components) {
        return new ItemStack(this, amount, components);
    }

    @Override
    public boolean is(Tag<ItemType> tag) {
        return baseType.is(tag);
    }

    @Override
    public boolean is(ItemTagKey tag) {
        return baseType.is(tag);
    }

    @Override
    public Collection<? extends Tag<ItemType>> tags() {
        return baseType.tags();
    }

    public Optional<ItemTool.Rule> customBlockToolRule(CustomBlockType blockType) {
        Objects.requireNonNull(blockType, "blockType");
        return customBlockToolRules.stream()
                .filter(rule -> matches(rule, blockType))
                .findFirst();
    }

    private static boolean matches(ItemTool.Rule rule, CustomBlockType blockType) {
        var blocks = rule.blocks();
        return blocks.tag().map(blockType::hasCustomTag).orElseGet(() -> blocks.values().contains(blockType.id()));
    }

    public static final class Builder {

        private final Key id;
        private final ItemType baseType;
        private final List<ItemTool.Rule> customBlockToolRules = new ArrayList<>();
        private ItemComponents defaultComponents = ItemComponents.EMPTY;

        private Builder(Key id, ItemType baseType) {
            this.id = Objects.requireNonNull(id, "id");
            this.baseType = Objects.requireNonNull(baseType, "baseType");
        }

        public Builder components(ItemComponents components) {
            this.defaultComponents = Objects.requireNonNull(components, "components");
            return this;
        }

        public Builder itemModel(Key model) {
            defaultComponents = defaultComponents.withKey(
                    ItemComponentKeys.ITEM_MODEL,
                    Objects.requireNonNull(model, "model"));
            return this;
        }

        public Builder useBaseItemModel() {
            return itemModel(baseType.key());
        }

        public Builder tool(ItemTool tool) {
            defaultComponents = defaultComponents.with(ItemComponentKeys.TOOL, Objects.requireNonNull(tool, "tool").toJson());
            return this;
        }

        public Builder customBlockToolRule(ItemTool.Rule rule) {
            customBlockToolRules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        public CustomItemType build() {
            return new CustomItemType(id, baseType, defaultComponents, customBlockToolRules);
        }
    }
}
