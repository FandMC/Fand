package io.fand.api.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fand.api.item.component.CustomModelData;
import io.fand.api.item.component.ItemComponentKeys;
import io.fand.api.item.component.ItemComponents;
import io.fand.api.item.component.ItemRarity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/**
 * An immutable item stack: a {@link ItemType}, a positive {@code amount}, and
 * a patch of modern vanilla data components.
 *
 * <p>The empty stack is represented by {@link #EMPTY}. Use {@code with*}
 * methods to build modified copies; no method mutates this instance.
 */
public record ItemStack(ItemType type, int amount, ItemComponents components) {

    /** Sentinel empty stack (type {@code null}, amount 0). */
    public static final ItemStack EMPTY = new ItemStack(null, 0, ItemComponents.EMPTY);

    public ItemStack(ItemType type, int amount) {
        this(type, amount, ItemComponents.EMPTY);
    }

    public ItemStack {
        components = components == null ? ItemComponents.EMPTY : components;
        if (type == null) {
            if (amount != 0) {
                throw new IllegalArgumentException("Empty stack must have amount 0");
            }
            components = ItemComponents.EMPTY;
        } else {
            Objects.requireNonNull(type, "type");
            if (amount < 1) {
                throw new IllegalArgumentException("Non-empty stack amount must be >= 1, got " + amount);
            }
            int maxStackSize = maxStackSize(type, components);
            if (amount > maxStackSize) {
                throw new IllegalArgumentException(
                        "Amount " + amount + " exceeds max stack size " + maxStackSize + " for " + type.key().asString());
            }
        }
    }

    public boolean isEmpty() {
        return type == null;
    }

    /** Effective maximum stack size, including a {@code max_stack_size} component override. */
    public int maxStackSize() {
        return isEmpty() ? 0 : maxStackSize(type, components);
    }

    public ItemStack withAmount(int newAmount) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, newAmount, components);
    }

    public Optional<JsonElement> component(Key key) {
        return components.get(key);
    }

    public boolean hasComponent(Key key) {
        return components.has(key);
    }

    public ItemStack withComponent(Key key, JsonElement value) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, amount, components.with(key, value));
    }

    /**
     * Drops an explicit override/removal for {@code key}, allowing the item
     * type's vanilla default component to show through again.
     */
    public ItemStack withoutComponent(Key key) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, amount, components.without(key));
    }

    /**
     * Forces {@code key} to be absent, even if the item type has a vanilla
     * default for it.
     */
    public ItemStack removeComponent(Key key) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, amount, components.remove(key));
    }

    public ItemStack withComponents(ItemComponents newComponents) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, amount, newComponents);
    }

    public ItemStack applyComponents(ItemComponents patch) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, amount, components.apply(patch));
    }

    public Optional<JsonObject> customData() {
        return component(ItemComponentKeys.CUSTOM_DATA)
                .filter(JsonElement::isJsonObject)
                .map(element -> element.getAsJsonObject().deepCopy());
    }

    public ItemStack withCustomData(JsonObject data) {
        Objects.requireNonNull(data, "data");
        return withComponent(ItemComponentKeys.CUSTOM_DATA, data);
    }

    public ItemStack withoutCustomData() {
        return withoutComponent(ItemComponentKeys.CUSTOM_DATA);
    }

    public Optional<Integer> maxStackSizeOverride() {
        return intComponent(ItemComponentKeys.MAX_STACK_SIZE);
    }

    public ItemStack withMaxStackSize(int maxStackSize) {
        if (maxStackSize < 1 || maxStackSize > 99) {
            throw new IllegalArgumentException("maxStackSize must be in 1..99");
        }
        return withComponent(ItemComponentKeys.MAX_STACK_SIZE, new JsonPrimitive(maxStackSize));
    }

    public ItemStack withoutMaxStackSize() {
        return withoutComponent(ItemComponentKeys.MAX_STACK_SIZE);
    }

    public Optional<Component> customName() {
        return textComponent(ItemComponentKeys.CUSTOM_NAME);
    }

    public ItemStack withCustomName(Component name) {
        return withTextComponent(ItemComponentKeys.CUSTOM_NAME, name);
    }

    public ItemStack withoutCustomName() {
        return withoutComponent(ItemComponentKeys.CUSTOM_NAME);
    }

    public Optional<Component> itemName() {
        return textComponent(ItemComponentKeys.ITEM_NAME);
    }

    public ItemStack withItemName(Component name) {
        return withTextComponent(ItemComponentKeys.ITEM_NAME, name);
    }

    public ItemStack withoutItemName() {
        return withoutComponent(ItemComponentKeys.ITEM_NAME);
    }

    public List<Component> lore() {
        return component(ItemComponentKeys.LORE)
                .filter(JsonElement::isJsonArray)
                .stream()
                .flatMap(element -> element.getAsJsonArray().asList().stream())
                .map(ItemStack::deserializeComponent)
                .toList();
    }

    public ItemStack withLore(List<Component> lore) {
        Objects.requireNonNull(lore, "lore");
        if (isEmpty()) {
            return EMPTY;
        }
        var lines = new JsonArray();
        for (var line : lore) {
            lines.add(serializeComponent(line));
        }
        return withComponent(ItemComponentKeys.LORE, lines);
    }

    public ItemStack withLore(Component... lore) {
        return withLore(List.of(lore));
    }

    public ItemStack addLoreLine(Component line) {
        var lines = new java.util.ArrayList<>(lore());
        lines.add(Objects.requireNonNull(line, "line"));
        return withLore(lines);
    }

    public ItemStack withoutLore() {
        return withoutComponent(ItemComponentKeys.LORE);
    }

    public Optional<Key> itemModel() {
        return component(ItemComponentKeys.ITEM_MODEL)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .map(Key::key);
    }

    public ItemStack withItemModel(Key model) {
        Objects.requireNonNull(model, "model");
        return withComponent(ItemComponentKeys.ITEM_MODEL, new JsonPrimitive(model.asString()));
    }

    public ItemStack withoutItemModel() {
        return withoutComponent(ItemComponentKeys.ITEM_MODEL);
    }

    public Optional<CustomModelData> customModelData() {
        return component(ItemComponentKeys.CUSTOM_MODEL_DATA).map(CustomModelData::fromJson);
    }

    public ItemStack withCustomModelData(CustomModelData data) {
        Objects.requireNonNull(data, "data");
        return withComponent(ItemComponentKeys.CUSTOM_MODEL_DATA, data.toJson());
    }

    public ItemStack withCustomModelData(int value) {
        return withCustomModelData(CustomModelData.ofInt(value));
    }

    public ItemStack withoutCustomModelData() {
        return withoutComponent(ItemComponentKeys.CUSTOM_MODEL_DATA);
    }

    public Optional<Boolean> enchantmentGlintOverride() {
        return component(ItemComponentKeys.ENCHANTMENT_GLINT_OVERRIDE)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsBoolean);
    }

    public ItemStack withEnchantmentGlintOverride(boolean glint) {
        return withComponent(ItemComponentKeys.ENCHANTMENT_GLINT_OVERRIDE, new JsonPrimitive(glint));
    }

    public ItemStack withoutEnchantmentGlintOverride() {
        return withoutComponent(ItemComponentKeys.ENCHANTMENT_GLINT_OVERRIDE);
    }

    public boolean unbreakable() {
        return hasComponent(ItemComponentKeys.UNBREAKABLE);
    }

    public ItemStack withUnbreakable(boolean unbreakable) {
        return unbreakable
                ? withComponent(ItemComponentKeys.UNBREAKABLE, new JsonObject())
                : withoutComponent(ItemComponentKeys.UNBREAKABLE);
    }

    public Optional<Integer> damage() {
        return intComponent(ItemComponentKeys.DAMAGE);
    }

    public ItemStack withDamage(int damage) {
        return withNonNegativeInt(ItemComponentKeys.DAMAGE, damage, "damage");
    }

    public ItemStack withoutDamage() {
        return withoutComponent(ItemComponentKeys.DAMAGE);
    }

    public Optional<Integer> maxDamage() {
        return intComponent(ItemComponentKeys.MAX_DAMAGE);
    }

    public ItemStack withMaxDamage(int maxDamage) {
        if (maxDamage < 1) {
            throw new IllegalArgumentException("maxDamage must be >= 1");
        }
        return withComponent(ItemComponentKeys.MAX_DAMAGE, new JsonPrimitive(maxDamage));
    }

    public ItemStack withoutMaxDamage() {
        return withoutComponent(ItemComponentKeys.MAX_DAMAGE);
    }

    public Optional<Integer> repairCost() {
        return intComponent(ItemComponentKeys.REPAIR_COST);
    }

    public ItemStack withRepairCost(int repairCost) {
        return withNonNegativeInt(ItemComponentKeys.REPAIR_COST, repairCost, "repairCost");
    }

    public ItemStack withoutRepairCost() {
        return withoutComponent(ItemComponentKeys.REPAIR_COST);
    }

    public Optional<ItemRarity> rarity() {
        return component(ItemComponentKeys.RARITY)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .map(ItemRarity::fromSerializedName);
    }

    public ItemStack withRarity(ItemRarity rarity) {
        Objects.requireNonNull(rarity, "rarity");
        return withComponent(ItemComponentKeys.RARITY, new JsonPrimitive(rarity.serializedName()));
    }

    public ItemStack withoutRarity() {
        return withoutComponent(ItemComponentKeys.RARITY);
    }

    public static ItemStack empty() {
        return EMPTY;
    }

    private Optional<Component> textComponent(Key key) {
        return component(key).map(ItemStack::deserializeComponent);
    }

    private ItemStack withTextComponent(Key key, Component component) {
        Objects.requireNonNull(component, "component");
        return withComponent(key, serializeComponent(component));
    }

    private Optional<Integer> intComponent(Key key) {
        return component(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsInt);
    }

    private ItemStack withNonNegativeInt(Key key, int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
        return withComponent(key, new JsonPrimitive(value));
    }

    private static JsonElement serializeComponent(Component component) {
        return GsonComponentSerializer.gson().serializeToTree(component);
    }

    private static Component deserializeComponent(JsonElement component) {
        return GsonComponentSerializer.gson().deserializeFromTree(component);
    }

    private static int maxStackSize(ItemType type, ItemComponents components) {
        return components.get(ItemComponentKeys.MAX_STACK_SIZE)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsInt)
                .orElseGet(type::maxStackSize);
    }
}
