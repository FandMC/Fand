package io.fand.testplugin;

import io.fand.api.block.custom.CustomBlockType;
import io.fand.api.item.custom.CustomItemType;
import io.fand.api.resourcepack.ResourcePackBuild;
import io.fand.api.resourcepack.ResourcePackService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import net.kyori.adventure.key.Key;

final class ExampleAssets {

    private static final String PACK_ID = "example";
    private static final String NAMESPACE = "fand-test-plugin";
    private static final List<String> STATIC_ASSETS = List.of(
            "items/color_block.json",
            "items/color_pickaxe.json",
            "models/block/color_block.json",
            "models/item/color_pickaxe.json",
            "textures/block/color_block_down.png",
            "textures/block/color_block_up.png",
            "textures/block/color_block_north.png",
            "textures/block/color_block_south.png",
            "textures/block/color_block_west.png",
            "textures/block/color_block_east.png",
            "textures/item/color_pickaxe.png");
    private static final List<String> NOTE_BLOCK_INSTRUMENTS = List.of(
            "harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime",
            "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling",
            "trumpet", "trumpet_exposed", "trumpet_oxidized", "trumpet_weathered", "zombie",
            "skeleton", "creeper", "dragon", "wither_skeleton", "piglin", "custom_head");

    private ExampleAssets() {
    }

    static ResourcePackBuild install(
            ResourcePackService resourcePacks,
            CustomItemType blockItem,
            CustomItemType pickaxe,
            CustomBlockType block
    ) {
        resourcePacks.create(PACK_ID, "Fand example custom items and blocks");
        for (var asset : STATIC_ASSETS) {
            copyResource(resourcePacks, asset);
        }
        resourcePacks.models(PACK_ID).carrierBlockState(
                block,
                noteBlockStateValues(),
                Key.key("minecraft:block/note_block"));

        requireModelIdentity(blockItem, block.id());
        requireModelIdentity(pickaxe, Key.key(NAMESPACE + ":color_pickaxe"));
        return resourcePacks.build(PACK_ID);
    }

    private static void copyResource(ResourcePackService resourcePacks, String asset) {
        var classpathPath = "/assets/" + NAMESPACE + "/" + asset;
        try (var input = ExampleAssets.class.getResourceAsStream(classpathPath)) {
            if (input == null) {
                throw new IllegalStateException("Missing bundled resource: " + classpathPath);
            }
            resourcePacks.write(PACK_ID, classpathPath.substring(1), input.readAllBytes());
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to read bundled resource " + classpathPath, failure);
        }
    }

    private static LinkedHashMap<String, List<String>> noteBlockStateValues() {
        var values = new LinkedHashMap<String, List<String>>();
        values.put("instrument", NOTE_BLOCK_INSTRUMENTS);
        values.put("note", java.util.stream.IntStream.rangeClosed(0, 24).mapToObj(Integer::toString).toList());
        values.put("powered", List.of("false", "true"));
        return values;
    }

    private static void requireModelIdentity(CustomItemType item, Key expected) {
        var actual = item.template().itemModel().orElseThrow();
        if (!actual.equals(expected)) {
            throw new IllegalStateException("Unexpected item model " + actual + "; expected " + expected);
        }
    }
}
