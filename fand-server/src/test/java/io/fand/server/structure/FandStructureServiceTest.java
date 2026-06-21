package io.fand.server.structure;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.structure.StructurePlacement;
import io.fand.api.structure.StructureVolume;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
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
