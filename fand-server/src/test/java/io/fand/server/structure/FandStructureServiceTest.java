package io.fand.server.structure;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.registry.RegistryReference;
import io.fand.api.structure.CustomStructure;
import io.fand.api.structure.CustomStructureSet;
import io.fand.api.structure.StructureGenerationPlacement;
import io.fand.api.structure.StructurePlacement;
import io.fand.api.structure.StructureVolume;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.kyori.adventure.key.Key;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;

final class FandStructureServiceTest {

    private static final Key TEMPLATE = Key.key("fand:test_template");
    private static final World WORLD = world(Key.key("minecraft:overworld"));
    private static final Location ORIGIN = new Location(WORLD, 0.0, 64.0, 0.0, 0.0F, 0.0F);

    @Test
    void returnsEmptyTemplateWithoutAttachedServer() {
        var service = new FandStructureService(() -> null);

        assertThat(service.template(TEMPLATE)).isEmpty();
    }

    @Test
    void asyncOperationsFailWithoutAttachedServer() {
        var service = new FandStructureService(() -> null);
        var volume = new StructureVolume(WORLD, 0, 64, 0, 1, 65, 1);

        assertThat(service.save(TEMPLATE, volume)).isCompletedExceptionally();
        assertThat(service.place(TEMPLATE, ORIGIN, StructurePlacement.defaults())).isCompletedExceptionally();
        assertThat(service.locate(Key.key("minecraft:village_plains"), ORIGIN, 8)).isCompletedExceptionally();
    }

    @Test
    void locateRejectsNegativeRadius() {
        var service = new FandStructureService(() -> null);

        assertThat(service.locate(Key.key("minecraft:village_plains"), ORIGIN, -1)).isCompletedExceptionally();
    }

    @Test
    void registersCustomStructuresAndStructureSetsWithoutAttachedServer() {
        var service = new FandStructureService(() -> null);
        var structure = CustomStructure.builder(Key.key("demo:hut"), Key.key("demo:hut_template"))
                .biomes(List.of(RegistryReference.all()))
                .build();
        var set = new CustomStructureSet(
                Key.key("demo:huts"),
                structure.key(),
                StructureGenerationPlacement.randomSpread(24, 8, 12345));

        var structureRegistration = service.registerStructure(structure);
        var setRegistration = service.registerStructureSet(set);

        assertThat(service.registeredStructure(structure.key())).contains(structure);
        assertThat(service.registeredStructureSet(set.key())).contains(set);
        assertThat(structureRegistration.active()).isTrue();
        assertThat(setRegistration.active()).isTrue();

        setRegistration.close();
        structureRegistration.close();

        assertThat(service.registeredStructure(structure.key())).isEmpty();
        assertThat(service.registeredStructureSet(set.key())).isEmpty();
    }

    @Test
    void spongeConversionKeepsPaletteZeroBlocks() {
        var schematic = new CompoundTag();
        schematic.putInt("Width", 1);
        schematic.putInt("Height", 1);
        schematic.putInt("Length", 1);
        var palette = new CompoundTag();
        palette.putInt("minecraft:stone", 0);
        schematic.put("Palette", palette);
        schematic.putByteArray("BlockData", new byte[] { 0 });

        var vanilla = invokeTag("spongeToVanilla", schematic);

        assertThat(vanilla.getListOrEmpty("blocks")).hasSize(1);
        assertThat(vanilla.getListOrEmpty("blocks").getCompoundOrEmpty(0).getIntOr("state", -1)).isZero();
    }

    @Test
    void spongeConversionAttachesBlockEntitiesAndEntities() {
        var schematic = new CompoundTag();
        schematic.putInt("Width", 1);
        schematic.putInt("Height", 1);
        schematic.putInt("Length", 1);
        var palette = new CompoundTag();
        palette.putInt("minecraft:chest", 0);
        schematic.put("Palette", palette);
        schematic.putByteArray("BlockData", new byte[] { 0 });

        var blockEntities = new ListTag();
        var chest = new CompoundTag();
        chest.putString("Id", "minecraft:chest");
        chest.putString("Lock", "test-lock");
        chest.putIntArray("Pos", new int[] { 0, 0, 0 });
        blockEntities.add(chest);
        schematic.put("BlockEntities", blockEntities);

        var entities = new ListTag();
        var item = new CompoundTag();
        item.putString("Id", "minecraft:item");
        item.put("Pos", doubleList(0.5, 0.0, 0.5));
        entities.add(item);
        schematic.put("Entities", entities);

        var vanilla = invokeTag("spongeToVanilla", schematic);
        var block = vanilla.getListOrEmpty("blocks").getCompoundOrEmpty(0);
        var blockNbt = block.getCompoundOrEmpty("nbt");
        var entityNbt = vanilla.getListOrEmpty("entities").getCompoundOrEmpty(0).getCompoundOrEmpty("nbt");

        assertThat(blockNbt.getStringOr("id", "")).isEqualTo("minecraft:chest");
        assertThat(blockNbt.getStringOr("Lock", "")).isEqualTo("test-lock");
        assertThat(entityNbt.getStringOr("id", "")).isEqualTo("minecraft:item");
    }

    @Test
    void vanillaExportWritesSpongeBlockEntitiesEntitiesAndAirDefault() {
        var vanilla = new CompoundTag();
        vanilla.put("size", intList(2, 1, 1));
        var palette = new ListTag();
        palette.add(blockState("minecraft:chest"));
        vanilla.put("palette", palette);

        var blocks = new ListTag();
        var block = new CompoundTag();
        block.put("pos", intList(1, 0, 0));
        block.putInt("state", 0);
        var chest = new CompoundTag();
        chest.putString("id", "minecraft:chest");
        block.put("nbt", chest);
        blocks.add(block);
        vanilla.put("blocks", blocks);

        var entities = new ListTag();
        var entity = new CompoundTag();
        entity.put("pos", doubleList(1.5, 0.0, 0.5));
        entity.put("blockPos", intList(1, 0, 0));
        var item = new CompoundTag();
        item.putString("id", "minecraft:item");
        entity.put("nbt", item);
        entities.add(entity);
        vanilla.put("entities", entities);

        var sponge = invokeTag("vanillaToSponge", vanilla);

        assertThat(sponge.getCompoundOrEmpty("Palette").getIntOr("minecraft:air", -1)).isZero();
        assertThat(sponge.getCompoundOrEmpty("Palette").getIntOr("minecraft:chest", -1)).isEqualTo(1);
        assertThat(sponge.getByteArray("BlockData").orElseThrow()).containsExactly((byte) 0, (byte) 1);
        var blockEntity = sponge.getListOrEmpty("BlockEntities").getCompoundOrEmpty(0);
        assertThat(blockEntity.getStringOr("Id", "")).isEqualTo("minecraft:chest");
        assertThat(blockEntity.getIntArray("Pos").orElseThrow()).containsExactly(1, 0, 0);
        assertThat(sponge.getListOrEmpty("Entities").getCompoundOrEmpty(0).getStringOr("Id", "")).isEqualTo("minecraft:item");
    }

    @Test
    void legacyAddBlocksUsePackedHighIdNibbles() {
        assertThat(invokeLegacyBlockId(new byte[] { 0x23, 0x00 }, new byte[] { 0x10 }, 0)).isEqualTo(0x23);
        assertThat(invokeLegacyBlockId(new byte[] { 0x23, 0x00 }, new byte[] { 0x10 }, 1)).isEqualTo(0x100);
    }

    @Test
    void bluImportReadsZippedBlueprintJson() throws Exception {
        var json = """
                {
                  "name": "island",
                  "xSize": 3,
                  "ySize": 3,
                  "zSize": 3,
                  "bedrock": [0, 0, 0],
                  "blocks": {
                    "[1,0,0]": {"blockData": "minecraft:stone"}
                  }
                }
                """;

        var vanilla = invokeReadTag(zipBlu("island", json), io.fand.api.structure.StructureFormat.BLU);

        assertThat(vanilla.getListOrEmpty("size").getIntOr(0, 0)).isEqualTo(3);
        assertThat(vanilla.getListOrEmpty("size").getIntOr(1, 0)).isEqualTo(3);
        assertThat(vanilla.getListOrEmpty("size").getIntOr(2, 0)).isEqualTo(3);
        assertThat(vanilla.getListOrEmpty("blocks")).hasSize(2);
        assertThat(vanilla.getListOrEmpty("blocks").compoundStream()
                .anyMatch(block -> block.getListOrEmpty("pos").getIntOr(0, -1) == 0
                        && block.getListOrEmpty("pos").getIntOr(1, -1) == 0
                        && block.getListOrEmpty("pos").getIntOr(2, -1) == 0))
                .isTrue();
    }

    @Test
    void bluExportWritesZippedBlueprintJson() throws Exception {
        var vanilla = newBaseVanilla();

        var data = invokeWriteTag(vanilla, io.fand.api.structure.StructureFormat.BLU);

        assertThat(new String(unzipBlu(data), StandardCharsets.UTF_8)).contains(
                "\"xSize\":1",
                "\"ySize\":1",
                "\"zSize\":1",
                "\"blocks\":{\"[0,0,0]\"",
                "\"minecraft:bedrock\"");
    }

    @Test
    void bluImportAcceptsLegacyBedrockOnlyArchive() throws Exception {
        var json = """
                {
                  "name": "island",
                  "xSize": 6,
                  "ySize": 10,
                  "zSize": 6,
                  "bedrock": [1, -5, -2]
                }
                """;

        var vanilla = invokeReadTag(zipBlu("island", json), io.fand.api.structure.StructureFormat.BLU);

        assertThat(vanilla.getListOrEmpty("size").getIntOr(0, 0)).isEqualTo(6);
        assertThat(vanilla.getListOrEmpty("size").getIntOr(1, 0)).isEqualTo(10);
        assertThat(vanilla.getListOrEmpty("size").getIntOr(2, 0)).isEqualTo(6);
        assertThat(vanilla.getListOrEmpty("blocks")).hasSize(1);
        assertThat(vanilla.getListOrEmpty("palette").getCompoundOrEmpty(0).getStringOr("Name", ""))
                .isEqualTo("minecraft:bedrock");
    }

    @Test
    void litematicImportReadsPaletteAndPackedBlockStates() throws Exception {
        var litematic = new CompoundTag();
        litematic.putInt("Version", 7);
        litematic.putInt("MinecraftDataVersion", 4440);
        litematic.put("Metadata", new CompoundTag());
        var region = new CompoundTag();
        region.put("Position", vectorTag(0, 0, 0));
        region.put("Size", vectorTag(2, 1, 1));
        var palette = new ListTag();
        palette.add(blockState("minecraft:air"));
        palette.add(blockState("minecraft:stone"));
        region.put("BlockStatePalette", palette);
        region.putLongArray("BlockStates", new long[] { 2L });
        var regions = new CompoundTag();
        regions.put("main", region);
        litematic.put("Regions", regions);

        var vanilla = invokeReadTag(writeCompressed(litematic), io.fand.api.structure.StructureFormat.LITEMATIC);

        assertThat(vanilla.getListOrEmpty("size").getIntOr(0, 0)).isEqualTo(2);
        assertThat(vanilla.getListOrEmpty("blocks")).hasSize(2);
        assertThat(vanilla.getListOrEmpty("palette").compoundStream().map(tag -> tag.getStringOr("Name", "")).toList())
                .contains("minecraft:air", "minecraft:stone");
    }

    @Test
    void litematicExportWritesRegionsPaletteAndPackedStates() throws Exception {
        var data = invokeWriteTag(newBaseVanilla(), io.fand.api.structure.StructureFormat.LITEMATIC);
        var root = NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
        var region = root.getCompoundOrEmpty("Regions").getCompoundOrEmpty("main");

        assertThat(root.getCompoundOrEmpty("Regions").keySet()).contains("main");
        assertThat(region.getListOrEmpty("BlockStatePalette")).isNotEmpty();
        assertThat(region.getLongArray("BlockStates").orElseThrow()).isNotEmpty();
    }

    private static World world(Key key) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[] { World.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "key" -> key;
                    case "audiences" -> List.of();
                    case "toString" -> "TestWorld[" + key.asString() + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
    }

    private static CompoundTag invokeTag(String methodName, CompoundTag tag) {
        try {
            Method method = FandStructureService.class.getDeclaredMethod(methodName, CompoundTag.class);
            method.setAccessible(true);
            return (CompoundTag) method.invoke(null, tag);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static CompoundTag invokeReadTag(byte[] data, io.fand.api.structure.StructureFormat format) {
        try {
            Method method = FandStructureService.class.getDeclaredMethod("readTag", byte[].class, io.fand.api.structure.StructureFormat.class);
            method.setAccessible(true);
            return (CompoundTag) method.invoke(null, data, format);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static byte[] invokeWriteTag(CompoundTag tag, io.fand.api.structure.StructureFormat format) {
        try {
            Method method = FandStructureService.class.getDeclaredMethod("writeTag", CompoundTag.class, io.fand.api.structure.StructureFormat.class);
            method.setAccessible(true);
            return (byte[]) method.invoke(null, tag, format);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static int invokeLegacyBlockId(byte[] blocks, byte[] addBlocks, int index) {
        try {
            Method method = FandStructureService.class.getDeclaredMethod("legacyBlockId", byte[].class, byte[].class, int.class);
            method.setAccessible(true);
            return (Integer) method.invoke(null, blocks, addBlocks, index);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static CompoundTag blockState(String name) {
        var tag = new CompoundTag();
        tag.putString("Name", name);
        return tag;
    }

    private static CompoundTag newBaseVanilla() {
        var vanilla = new CompoundTag();
        vanilla.put("size", intList(1, 1, 1));
        var palette = new ListTag();
        palette.add(blockState("minecraft:bedrock"));
        vanilla.put("palette", palette);
        var blocks = new ListTag();
        var block = new CompoundTag();
        block.put("pos", intList(0, 0, 0));
        block.putInt("state", 0);
        blocks.add(block);
        vanilla.put("blocks", blocks);
        return vanilla;
    }

    private static byte[] zipBlu(String name, String json) throws Exception {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(json.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static byte[] unzipBlu(byte[] data) throws Exception {
        try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
            var entry = zip.getNextEntry();
            assertThat(entry).isNotNull();
            return zip.readAllBytes();
        }
    }

    private static byte[] writeCompressed(CompoundTag tag) throws Exception {
        var out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, out);
        return out.toByteArray();
    }

    private static CompoundTag vectorTag(int x, int y, int z) {
        var tag = new CompoundTag();
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        return tag;
    }

    private static ListTag intList(int... values) {
        var list = new ListTag();
        for (int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    private static ListTag doubleList(double... values) {
        var list = new ListTag();
        for (double value : values) {
            list.add(DoubleTag.valueOf(value));
        }
        return list;
    }
}
