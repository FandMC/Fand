package io.fand.api.resourcepack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fand.api.block.custom.CustomBlockType;
import io.fand.api.item.custom.CustomItemType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Generates modern item definitions and model JSON into a managed resource pack.
 *
 * <p>This mirrors the model-provider part of mod data generation. Texture PNG files
 * remain ordinary resources and can be written with {@link #texture(Key, byte[])}.
 */
public final class ResourcePackModelGenerator {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final ResourcePackService resourcePacks;
    private final String packId;

    private ResourcePackModelGenerator(ResourcePackService resourcePacks, String packId) {
        this.resourcePacks = Objects.requireNonNull(resourcePacks, "resourcePacks");
        this.packId = Objects.requireNonNull(packId, "packId");
    }

    public static ResourcePackModelGenerator of(ResourcePackService resourcePacks, String packId) {
        return new ResourcePackModelGenerator(resourcePacks, packId);
    }

    public void flatItem(CustomItemType item, Key texture) {
        itemModel(item, Key.key("minecraft:item/generated"), Map.of("layer0", texture));
    }

    public void handheldItem(CustomItemType item, Key texture) {
        itemModel(item, Key.key("minecraft:item/handheld"), Map.of("layer0", texture));
    }

    public void itemModel(CustomItemType item, Key parent, Map<String, Key> textures) {
        Objects.requireNonNull(item, "item");
        var definitionKey = itemModelKey(item);
        var modelKey = itemResourceModelKey(definitionKey);
        model(modelKey, parent, textures);
        plainItemDefinition(definitionKey, modelKey);
    }

    public void itemDefinition(CustomItemType item, JsonElement modelDefinition) {
        Objects.requireNonNull(item, "item");
        itemDefinition(itemModelKey(item), modelDefinition);
    }

    public void itemDefinition(Key itemModel, JsonElement modelDefinition) {
        Objects.requireNonNull(itemModel, "itemModel");
        Objects.requireNonNull(modelDefinition, "modelDefinition");
        var root = new JsonObject();
        root.add("model", modelDefinition.deepCopy());
        writeJson(assetPath(itemModel, "items", ".json"), root);
    }

    public void plainItemDefinition(Key itemModel, Key model) {
        var definition = new JsonObject();
        definition.addProperty("type", "minecraft:model");
        definition.addProperty("model", model.asString());
        itemDefinition(itemModel, definition);
    }

    public void cubeAllBlock(CustomBlockType block, Key texture) {
        Objects.requireNonNull(block, "block");
        model(blockModelKey(block), Key.key("minecraft:block/cube_all"), Map.of("all", texture));
        simpleBlockState(block);
    }

    public void simpleBlockState(CustomBlockType block) {
        Objects.requireNonNull(block, "block");
        var apply = new JsonObject();
        apply.addProperty("model", blockModelKey(block).asString());
        var variants = new JsonObject();
        variants.add("", apply);
        var root = new JsonObject();
        root.add("variants", variants);
        blockState(block.id(), root);
    }

    /**
     * Writes the complete variant table for a vanilla carrier block.
     *
     * <p>The custom model is assigned only to the exact state declared by
     * {@link CustomBlockType#baseStateProperties()}; every other state keeps
     * {@code fallbackModel}. The caller must provide every property and value
     * accepted by the carrier block, in the order used by its blockstate JSON.
     */
    public void carrierBlockState(
            CustomBlockType block,
            Map<String, ? extends Collection<String>> propertyValues,
            Key fallbackModel
    ) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(propertyValues, "propertyValues");
        Objects.requireNonNull(fallbackModel, "fallbackModel");
        if (block.baseStateProperties().isEmpty()) {
            throw new IllegalArgumentException("Carrier block must declare at least one state property");
        }

        var normalized = new LinkedHashMap<String, java.util.List<String>>();
        propertyValues.forEach((property, values) -> {
            var name = Objects.requireNonNull(property, "property");
            var entries = java.util.List.copyOf(Objects.requireNonNull(values, "values"));
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("Carrier property has no values: " + name);
            }
            if (new java.util.LinkedHashSet<>(entries).size() != entries.size()) {
                throw new IllegalArgumentException("Carrier property contains duplicate values: " + name);
            }
            entries.forEach(value -> {
                if (Objects.requireNonNull(value, "property value").isBlank()) {
                    throw new IllegalArgumentException("Carrier property value cannot be blank: " + name);
                }
            });
            normalized.put(name, entries);
        });
        for (var state : block.baseStateProperties().entrySet()) {
            var values = normalized.get(state.getKey());
            if (values == null || !values.contains(state.getValue())) {
                throw new IllegalArgumentException("Carrier state is missing from property values: "
                        + state.getKey() + "=" + state.getValue());
            }
        }

        var variants = new JsonObject();
        appendCarrierVariants(
                variants,
                normalized.entrySet().stream().toList(),
                0,
                new LinkedHashMap<>(),
                block,
                fallbackModel);
        var root = new JsonObject();
        root.add("variants", variants);
        blockState(block.baseType().key(), root);
    }

    /** Writes a complete blockstate definition, including carrier definitions in the minecraft namespace. */
    public void blockState(Key block, JsonElement definition) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(definition, "definition");
        writeJson(assetPath(block, "blockstates", ".json"), definition);
    }

    public void blockItem(CustomItemType item, CustomBlockType block) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(block, "block");
        plainItemDefinition(itemModelKey(item), blockModelKey(block));
    }

    public void model(Key model, Key parent, Map<String, Key> textures) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(textures, "textures");
        var json = new JsonObject();
        json.addProperty("parent", parent.asString());
        var textureJson = new JsonObject();
        textures.forEach((slot, texture) -> textureJson.addProperty(
                Objects.requireNonNull(slot, "texture slot"),
                Objects.requireNonNull(texture, "texture").asString()));
        json.add("textures", textureJson);
        rawModel(model, json);
    }

    public void rawModel(Key model, JsonElement json) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(json, "json");
        writeJson(assetPath(model, "models", ".json"), json);
    }

    public void texture(Key texture, byte[] png) {
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(png, "png");
        if (png.length < PNG_SIGNATURE.length) {
            throw new IllegalArgumentException("Texture is not a PNG: " + texture.asString());
        }
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (png[index] != PNG_SIGNATURE[index]) {
                throw new IllegalArgumentException("Texture is not a PNG: " + texture.asString());
            }
        }
        resourcePacks.write(packId, assetPath(texture, "textures", ".png"), png.clone());
    }

    private void writeJson(String path, JsonElement json) {
        resourcePacks.writeJson(packId, path, json);
    }

    private static void appendCarrierVariants(
            JsonObject variants,
            java.util.List<Map.Entry<String, java.util.List<String>>> properties,
            int index,
            LinkedHashMap<String, String> state,
            CustomBlockType block,
            Key fallbackModel
    ) {
        if (index == properties.size()) {
            var apply = new JsonObject();
            apply.addProperty("model", matchesCarrierState(state, block) ? blockModelKey(block).asString() : fallbackModel.asString());
            variants.add(variantKey(state), apply);
            return;
        }
        var property = properties.get(index);
        for (var value : property.getValue()) {
            state.put(property.getKey(), value);
            appendCarrierVariants(variants, properties, index + 1, state, block, fallbackModel);
        }
        state.remove(property.getKey());
    }

    private static boolean matchesCarrierState(Map<String, String> state, CustomBlockType block) {
        return block.baseStateProperties().entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(state.get(entry.getKey())));
    }

    private static String variantKey(Map<String, String> state) {
        return state.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static Key itemModelKey(CustomItemType item) {
        return item.template().itemModel().orElse(item.id());
    }

    private static Key blockModelKey(CustomBlockType block) {
        return Key.key(block.id().namespace(), "block/" + block.id().value());
    }

    private static Key itemResourceModelKey(Key itemModel) {
        return Key.key(itemModel.namespace(), "item/" + itemModel.value());
    }

    private static String assetPath(Key key, String directory, String extension) {
        return "assets/" + key.namespace() + "/" + directory + "/" + key.value() + extension;
    }
}
