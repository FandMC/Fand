package io.fand.server.structure;

import com.mojang.datafixers.util.Pair;
import io.fand.api.structure.StructureFormat;
import io.fand.api.structure.StructureMirror;
import io.fand.api.structure.StructurePlacement;
import io.fand.api.structure.StructureProjection;
import io.fand.api.structure.StructureRotation;
import io.fand.api.structure.StructureService;
import io.fand.api.structure.StructureTemplate;
import io.fand.api.structure.StructureVolume;
import io.fand.api.world.Location;
import io.fand.server.world.FandWorld;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;

public final class FandStructureService implements StructureService {

    private final Supplier<MinecraftServer> server;

    public FandStructureService(Supplier<MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public Optional<StructureTemplate> template(Key key) {
        Objects.requireNonNull(key, "key");
        var current = server.get();
        if (current == null) {
            return Optional.empty();
        }
        return callOnServerThread(current, () -> current.getStructureManager()
                .get(identifier(key))
                .map(template -> view(key, template)));
    }

    @Override
    public CompletableFuture<Boolean> save(Key key, StructureVolume volume) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(volume, "volume");
        var current = server.get();
        if (current == null) {
            return notAttached();
        }
        return submit(current, () -> saveOnServerThread(key, volume));
    }

    @Override
    public CompletableFuture<Optional<StructureProjection>> exportTemplate(Key key, StructureFormat format) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(format, "format");
        var current = server.get();
        if (current == null) {
            return notAttached();
        }
        return submit(current, () -> exportOnServerThread(key, format));
    }

    @Override
    public CompletableFuture<Boolean> importTemplate(Key key, StructureProjection projection) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(projection, "projection");
        var current = server.get();
        if (current == null) {
            return notAttached();
        }
        return submit(current, () -> importOnServerThread(current, key, projection));
    }

    @Override
    public CompletableFuture<Optional<StructureProjection>> load(Path path, StructureFormat format) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Files.isRegularFile(path)) {
                    return Optional.empty();
                }
                var detected = format == StructureFormat.AUTO ? detectFormat(path) : format;
                return Optional.of(StructureProjection.of(detected, Files.readAllBytes(path))
                        .withName(path.getFileName().toString()));
            } catch (IOException failure) {
                throw new IllegalStateException("Could not load structure projection: " + path, failure);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> save(Key key, Path path, StructureFormat format) {
        Objects.requireNonNull(path, "path");
        return exportTemplate(key, format).thenApply(projection -> {
            if (projection.isEmpty()) {
                return false;
            }
            try {
                var parent = path.toAbsolutePath().normalize().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(path, projection.orElseThrow().data());
                return true;
            } catch (IOException failure) {
                throw new IllegalStateException("Could not save structure projection: " + path, failure);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> place(Key key, Location origin, StructurePlacement placement) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(placement, "placement");
        var current = server.get();
        if (current == null) {
            return notAttached();
        }
        return submit(current, () -> placeOnServerThread(key, origin, placement));
    }

    @Override
    public CompletableFuture<Optional<Location>> locate(Key structure, Location origin, int radius) {
        Objects.requireNonNull(structure, "structure");
        Objects.requireNonNull(origin, "origin");
        if (radius < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("radius must be non-negative"));
        }
        var current = server.get();
        if (current == null) {
            return notAttached();
        }
        return submit(current, () -> locateOnServerThread(structure, origin, radius));
    }

    private boolean saveOnServerThread(Key key, StructureVolume volume) {
        var level = level(volume.world());
        var minX = Math.min(volume.minX(), volume.maxX());
        var minY = Math.min(volume.minY(), volume.maxY());
        var minZ = Math.min(volume.minZ(), volume.maxZ());
        var maxX = Math.max(volume.minX(), volume.maxX());
        var maxY = Math.max(volume.minY(), volume.maxY());
        var maxZ = Math.max(volume.minZ(), volume.maxZ());
        var size = new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
        if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1) {
            return false;
        }

        var manager = level.getStructureManager();
        var id = identifier(key);
        var template = manager.getOrCreate(id);
        template.fillFromWorld(level, new BlockPos(minX, minY, minZ), size, true, List.of(Blocks.STRUCTURE_VOID));
        template.setAuthor("Fand");
        return manager.save(id);
    }

    private Optional<StructureProjection> exportOnServerThread(Key key, StructureFormat format) {
        var template = server.get().getStructureManager().get(identifier(key)).orElse(null);
        if (template == null) {
            return Optional.empty();
        }
        var actualFormat = format == StructureFormat.AUTO ? StructureFormat.VANILLA_NBT : format;
        var tag = template.save(new CompoundTag());
        try {
            return Optional.of(new StructureProjection(actualFormat, writeTag(tag, actualFormat), Optional.of(key), Optional.empty()));
        } catch (IOException failure) {
            throw new IllegalStateException("Could not export structure " + key.asString() + " as " + actualFormat, failure);
        }
    }

    private boolean importOnServerThread(MinecraftServer server, Key key, StructureProjection projection) {
        try {
            var format = projection.format() == StructureFormat.AUTO ? detectFormat(projection.data()) : projection.format();
            var tag = readTag(projection.data(), format);
            var template = server.getStructureManager().getOrCreate(identifier(key));
            template.load(server.registryAccess().lookupOrThrow(Registries.BLOCK), tag);
            template.setAuthor("Fand");
            return true;
        } catch (IOException failure) {
            throw new IllegalArgumentException("Invalid structure projection for " + key.asString(), failure);
        }
    }

    private boolean placeOnServerThread(Key key, Location origin, StructurePlacement placement) {
        var level = level(origin.world());
        var template = level.getStructureManager().get(identifier(key)).orElse(null);
        if (template == null) {
            return false;
        }
        var pos = new BlockPos(origin.blockX(), origin.blockY(), origin.blockZ());
        var settings = new StructurePlaceSettings()
                .setRotation(rotation(placement.rotation()))
                .setMirror(mirror(placement.mirrorMode()))
                .setIgnoreEntities(!placement.includeEntities())
                .setKnownShape(placement.knownShape());
        if (placement.integrity() < 1.0F) {
            settings.setRandom(RandomSource.create(placement.seed()));
            settings.addProcessor(new BlockRotProcessor(placement.integrity()));
        }
        if (placement.ignoreStructureVoid()) {
            settings.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        }
        return template.placeInWorld(
                level,
                pos,
                pos,
                settings,
                net.minecraft.world.level.block.entity.StructureBlockEntity.createRandom(placement.seed()),
                placement.updateFlags());
    }

    private Optional<Location> locateOnServerThread(Key structure, Location origin, int radius) {
        var level = level(origin.world());
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var holder = registry.get(ResourceKey.create(Registries.STRUCTURE, identifier(structure))).orElse(null);
        if (holder == null) {
            return Optional.empty();
        }

        Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource()
                .getGenerator()
                .findNearestMapStructure(
                        level,
                        HolderSet.direct(holder),
                        new BlockPos(origin.blockX(), origin.blockY(), origin.blockZ()),
                        radius,
                        false);
        if (nearest == null) {
            return Optional.empty();
        }
        var pos = nearest.getFirst();
        return Optional.of(new Location(origin.world(), pos.getX(), pos.getY(), pos.getZ(), origin.yaw(), origin.pitch()));
    }

    private static StructureTemplate view(Key key, net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template) {
        var size = template.getSize();
        return new StructureTemplate(key, size.getX(), size.getY(), size.getZ());
    }

    private static byte[] writeTag(CompoundTag tag, StructureFormat format) throws IOException {
        return switch (format) {
            case VANILLA_NBT, SPONGE_SCHEMATIC, WORLDEDIT_SCHEMATIC, AUTO -> {
                var out = new ByteArrayOutputStream();
                NbtIo.writeCompressed(format == StructureFormat.VANILLA_NBT ? tag : vanillaToSponge(tag), out);
                yield out.toByteArray();
            }
            case VANILLA_SNBT -> NbtUtils.structureToSnbt(tag.copy()).getBytes(StandardCharsets.UTF_8);
        };
    }

    private static CompoundTag readTag(byte[] data, StructureFormat format) throws IOException {
        return switch (format) {
            case VANILLA_NBT -> NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
            case VANILLA_SNBT -> snbtToStructure(data);
            case SPONGE_SCHEMATIC, WORLDEDIT_SCHEMATIC -> schematicToVanilla(NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap()));
            case AUTO -> readTag(data, detectFormat(data));
        };
    }

    private static CompoundTag snbtToStructure(byte[] data) throws IOException {
        try {
            return NbtUtils.snbtToStructure(new String(data, StandardCharsets.UTF_8));
        } catch (Exception failure) {
            throw new IOException("Invalid vanilla SNBT structure", failure);
        }
    }

    private static StructureFormat detectFormat(Path path) {
        var name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".snbt")) {
            return StructureFormat.VANILLA_SNBT;
        }
        if (name.endsWith(".schem")) {
            return StructureFormat.SPONGE_SCHEMATIC;
        }
        if (name.endsWith(".schematic")) {
            return StructureFormat.WORLDEDIT_SCHEMATIC;
        }
        return StructureFormat.VANILLA_NBT;
    }

    private static StructureFormat detectFormat(byte[] data) {
        if (data.length > 0 && (data[0] == '{' || data[0] == '[')) {
            return StructureFormat.VANILLA_SNBT;
        }
        try {
            var tag = NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
            if (tag.contains("Palette") || tag.contains("BlockData") || tag.contains("Blocks")) {
                return tag.contains("Palette") ? StructureFormat.SPONGE_SCHEMATIC : StructureFormat.WORLDEDIT_SCHEMATIC;
            }
        } catch (IOException ignored) {
        }
        return StructureFormat.VANILLA_NBT;
    }

    private static CompoundTag schematicToVanilla(CompoundTag schematic) throws IOException {
        if (schematic.contains("Palette") && schematic.contains("BlockData")) {
            return spongeToVanilla(schematic);
        }
        if (schematic.contains("Blocks")) {
            return legacyWorldEditToVanilla(schematic);
        }
        if (schematic.contains("Schematic")) {
            return schematicToVanilla(schematic.getCompoundOrEmpty("Schematic"));
        }
        return schematic;
    }

    private static CompoundTag spongeToVanilla(CompoundTag schematic) throws IOException {
        var width = schematic.getIntOr("Width", schematic.getShortOr("Width", (short) 0));
        var height = schematic.getIntOr("Height", schematic.getShortOr("Height", (short) 0));
        var length = schematic.getIntOr("Length", schematic.getShortOr("Length", (short) 0));
        requirePositiveSize(width, height, length);

        var palette = schematic.getCompoundOrEmpty("Palette");
        var paletteList = paletteToList(palette);
        var blockData = schematic.getByteArray("BlockData").orElseThrow(() -> new IOException("Sponge schematic missing BlockData"));
        var states = decodeVarInts(blockData, width * height * length);

        var vanilla = newBaseTemplate(width, height, length);
        vanilla.put("palette", paletteList);
        vanilla.put("blocks", indexedBlocks(width, height, length, states, blockEntitiesByIndex(schematic, width, height, length)));
        copySchematicMetadata(schematic, vanilla);
        copyEntities(schematic, vanilla);
        return vanilla;
    }

    private static CompoundTag legacyWorldEditToVanilla(CompoundTag schematic) throws IOException {
        var width = schematic.getShortOr("Width", (short) 0);
        var height = schematic.getShortOr("Height", (short) 0);
        var length = schematic.getShortOr("Length", (short) 0);
        requirePositiveSize(width, height, length);

        var blocks = schematic.getByteArray("Blocks").orElseThrow(() -> new IOException("Legacy schematic missing Blocks"));
        var data = schematic.getByteArray("Data").orElse(new byte[blocks.length]);
        var addBlocks = schematic.getByteArray("AddBlocks").orElse(null);
        if (blocks.length != width * height * length) {
            throw new IOException("Legacy schematic block array size mismatch");
        }
        var palette = new ListTag();
        var indices = new int[blocks.length];
        var stateIds = new java.util.LinkedHashMap<String, Integer>();
        for (int i = 0; i < blocks.length; i++) {
            int legacyId = legacyBlockId(blocks, addBlocks, i);
            int legacyData = i < data.length ? data[i] & 0x0F : 0;
            var state = Block.BLOCK_STATE_REGISTRY.byId((legacyId << 4) | legacyData);
            if (state == null) {
                state = Blocks.AIR.defaultBlockState();
            }
            var encoded = NbtUtils.writeBlockState(state);
            var packedKey = encoded.toString();
            var index = stateIds.computeIfAbsent(packedKey, ignored -> {
                palette.add(encoded);
                return palette.size() - 1;
            });
            indices[i] = index;
        }

        var vanilla = newBaseTemplate(width, height, length);
        vanilla.put("palette", palette);
        vanilla.put("blocks", indexedBlocks(width, height, length, indices, blockEntitiesByIndex(schematic, width, height, length)));
        copySchematicMetadata(schematic, vanilla);
        copyEntities(schematic, vanilla);
        return vanilla;
    }

    private static CompoundTag vanillaToSponge(CompoundTag vanilla) {
        var sponge = new CompoundTag();
        var size = vanilla.getListOrEmpty("size");
        int width = size.getIntOr(0, 0);
        int height = size.getIntOr(1, 0);
        int length = size.getIntOr(2, 0);
        int volume = Math.max(0, width * height * length);
        sponge.putInt("Version", 2);
        sponge.putInt("DataVersion", vanilla.getIntOr("DataVersion", currentDataVersion()));
        sponge.putInt("Width", width);
        sponge.putInt("Height", height);
        sponge.putInt("Length", length);

        var palette = new CompoundTag();
        var paletteList = vanillaPalette(vanilla);
        int airPaletteIndex = findPaletteIndex(paletteList, "minecraft:air");
        int airState = 0;
        int[] remappedStates = new int[paletteList.size()];
        palette.putInt("minecraft:air", airState);
        if (airPaletteIndex >= 0) {
            remappedStates[airPaletteIndex] = airState;
        }
        int nextState = 1;
        for (int i = 0; i < paletteList.size(); i++) {
            if (i == airPaletteIndex) {
                continue;
            }
            var state = blockStateString(paletteList.getCompoundOrEmpty(i));
            remappedStates[i] = nextState;
            palette.putInt(state, nextState++);
        }
        sponge.put("Palette", palette);
        sponge.putInt("PaletteMax", nextState);

        var states = new int[volume];
        java.util.Arrays.fill(states, airState);
        var blockEntities = new ListTag();
        vanilla.getListOrEmpty("blocks").compoundStream().forEach(block -> {
            var pos = block.getListOrEmpty("pos");
            int index = blockIndex(width, length, pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0));
            if (index >= 0 && index < states.length) {
                int vanillaState = block.getIntOr("state", 0);
                states[index] = vanillaState >= 0 && vanillaState < remappedStates.length
                        ? remappedStates[vanillaState]
                        : airState;
                block.getCompound("nbt").ifPresent(nbt -> blockEntities.add(spongeBlockEntity(nbt, pos)));
            }
        });
        sponge.putByteArray("BlockData", encodeVarInts(states));
        if (!blockEntities.isEmpty()) {
            sponge.put("BlockEntities", blockEntities);
        }
        var entities = spongeEntities(vanilla.getListOrEmpty("entities"));
        if (!entities.isEmpty()) {
            sponge.put("Entities", entities);
        }
        copySchematicMetadata(vanilla, sponge);
        writeZeroOffsetDefaults(sponge);
        return sponge;
    }

    private static ListTag paletteToList(CompoundTag palette) throws IOException {
        var entries = new java.util.ArrayList<java.util.Map.Entry<String, net.minecraft.nbt.Tag>>(palette.entrySet());
        entries.sort(java.util.Comparator.comparingInt(entry -> entry.getValue().asInt().orElse(Integer.MAX_VALUE)));
        var list = new ListTag();
        for (var entry : entries) {
            list.add(blockStateTag(entry.getKey()));
        }
        if (list.isEmpty()) {
            throw new IOException("Schematic palette must not be empty");
        }
        return list;
    }

    private static CompoundTag blockStateTag(String state) {
        var tag = new CompoundTag();
        var bracket = state.indexOf('[');
        var name = bracket >= 0 ? state.substring(0, bracket) : state;
        tag.putString("Name", name);
        if (bracket >= 0 && state.endsWith("]")) {
            var properties = new CompoundTag();
            var body = state.substring(bracket + 1, state.length() - 1);
            if (!body.isBlank()) {
                for (var part : body.split(",")) {
                    var split = part.split("=", 2);
                    if (split.length == 2) {
                        properties.putString(split[0], split[1]);
                    }
                }
            }
            if (!properties.isEmpty()) {
                tag.put("Properties", properties);
            }
        }
        return tag;
    }

    private static String blockStateString(CompoundTag tag) {
        var builder = new StringBuilder(tag.getStringOr("Name", "minecraft:air"));
        tag.getCompound("Properties").ifPresent(properties -> {
            var keys = new java.util.ArrayList<>(properties.keySet());
            java.util.Collections.sort(keys);
            if (!keys.isEmpty()) {
                builder.append('[');
                for (int i = 0; i < keys.size(); i++) {
                    if (i > 0) {
                        builder.append(',');
                    }
                    var key = keys.get(i);
                    builder.append(key).append('=').append(properties.getStringOr(key, ""));
                }
                builder.append(']');
            }
        });
        return builder.toString();
    }

    private static ListTag vanillaPalette(CompoundTag vanilla) {
        var palette = vanilla.getList("palette");
        if (palette.isPresent()) {
            return palette.get();
        }
        var palettes = vanilla.getListOrEmpty("palettes");
        return palettes.getListOrEmpty(0);
    }

    private static int findPaletteIndex(ListTag palette, String stateName) {
        for (int i = 0; i < palette.size(); i++) {
            if (stateName.equals(blockStateString(palette.getCompoundOrEmpty(i)))) {
                return i;
            }
        }
        return -1;
    }

    private static int legacyBlockId(byte[] blocks, byte[] addBlocks, int index) {
        int id = blocks[index] & 0xFF;
        if (addBlocks == null || (index >> 1) >= addBlocks.length) {
            return id;
        }
        int packed = addBlocks[index >> 1] & 0xFF;
        int extra = (index & 1) == 0 ? packed & 0x0F : (packed >> 4) & 0x0F;
        return id | (extra << 8);
    }

    private static ListTag indexedBlocks(int width, int height, int length, int[] states, java.util.Map<Integer, CompoundTag> blockEntities) throws IOException {
        if (states.length != width * height * length) {
            throw new IOException("Block data length does not match schematic dimensions");
        }
        var blocks = new ListTag();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = blockIndex(width, length, x, y, z);
                    int state = states[index];
                    var block = new CompoundTag();
                    block.put("pos", intList(x, y, z));
                    block.putInt("state", state);
                    var blockEntity = blockEntities.get(index);
                    if (blockEntity != null) {
                        block.put("nbt", blockEntity.copy());
                    }
                    blocks.add(block);
                }
            }
        }
        return blocks;
    }

    private static int blockIndex(int width, int length, int x, int y, int z) {
        return (y * length + z) * width + x;
    }

    private static java.util.Map<Integer, CompoundTag> blockEntitiesByIndex(CompoundTag schematic, int width, int height, int length) {
        var blockEntities = new java.util.HashMap<Integer, CompoundTag>();
        copyBlockEntityList(schematic.getListOrEmpty("BlockEntities"), width, height, length, blockEntities);
        copyBlockEntityList(schematic.getListOrEmpty("TileEntities"), width, height, length, blockEntities);
        copyBlockEntityList(schematic.getListOrEmpty("block_entities"), width, height, length, blockEntities);
        return blockEntities;
    }

    private static void copyBlockEntityList(ListTag source, int width, int height, int length, java.util.Map<Integer, CompoundTag> target) {
        source.compoundStream().forEach(blockEntity -> {
            int[] pos = blockEntityPosition(blockEntity);
            if (!insideVolume(pos[0], pos[1], pos[2], width, height, length)) {
                return;
            }
            target.put(blockIndex(width, length, pos[0], pos[1], pos[2]), normalizeBlockEntityNbt(blockEntity));
        });
    }

    private static int[] blockEntityPosition(CompoundTag blockEntity) {
        var array = blockEntity.getIntArray("Pos").orElse(null);
        if (array != null && array.length >= 3) {
            return new int[] { array[0], array[1], array[2] };
        }
        var pos = blockEntity.getList("Pos").or(() -> blockEntity.getList("pos")).orElse(null);
        if (pos != null && pos.size() >= 3) {
            return new int[] { pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0) };
        }
        return new int[] {
                blockEntity.getIntOr("x", 0),
                blockEntity.getIntOr("y", 0),
                blockEntity.getIntOr("z", 0)
        };
    }

    private static boolean insideVolume(int x, int y, int z, int width, int height, int length) {
        return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < length;
    }

    private static CompoundTag normalizeBlockEntityNbt(CompoundTag source) {
        var nbt = source.copy();
        if (!nbt.contains("id")) {
            source.getString("Id").ifPresent(id -> nbt.putString("id", id));
        }
        return nbt;
    }

    private static CompoundTag spongeBlockEntity(CompoundTag vanillaNbt, ListTag pos) {
        var blockEntity = vanillaNbt.copy();
        blockEntity.putIntArray("Pos", new int[] {
                pos.getIntOr(0, 0),
                pos.getIntOr(1, 0),
                pos.getIntOr(2, 0)
        });
        if (!blockEntity.contains("Id")) {
            vanillaNbt.getString("id").ifPresent(id -> blockEntity.putString("Id", id));
        }
        return blockEntity;
    }

    private static CompoundTag newBaseTemplate(int width, int height, int length) {
        var tag = new CompoundTag();
        tag.put("size", intList(width, height, length));
        tag.put("entities", new ListTag());
        tag.putInt("DataVersion", currentDataVersion());
        return tag;
    }

    private static int currentDataVersion() {
        try {
            return net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version();
        } catch (IllegalStateException ignored) {
            return 0;
        }
    }

    private static void copyEntities(CompoundTag source, CompoundTag target) {
        var entities = new ListTag();
        source.getListOrEmpty("entities").compoundStream()
                .map(FandStructureService::vanillaEntity)
                .forEach(entities::add);
        source.getListOrEmpty("Entities").compoundStream()
                .map(FandStructureService::vanillaEntity)
                .forEach(entities::add);
        target.put("entities", entities);
    }

    private static CompoundTag vanillaEntity(CompoundTag source) {
        if (source.contains("nbt")) {
            var entity = source.copy();
            var pos = entityPosition(entity);
            if (!entity.contains("pos")) {
                entity.put("pos", doubleList(pos[0], pos[1], pos[2]));
            }
            if (!entity.contains("blockPos")) {
                entity.put("blockPos", intList((int) Math.floor(pos[0]), (int) Math.floor(pos[1]), (int) Math.floor(pos[2])));
            }
            return entity;
        }

        var pos = entityPosition(source);
        var entity = new CompoundTag();
        entity.put("pos", doubleList(pos[0], pos[1], pos[2]));
        entity.put("blockPos", intList(
                source.getIntOr("TileX", (int) Math.floor(pos[0])),
                source.getIntOr("TileY", (int) Math.floor(pos[1])),
                source.getIntOr("TileZ", (int) Math.floor(pos[2]))));
        entity.put("nbt", normalizeEntityNbt(source));
        return entity;
    }

    private static CompoundTag normalizeEntityNbt(CompoundTag source) {
        var nbt = source.copy();
        if (!nbt.contains("id")) {
            source.getString("Id").ifPresent(id -> nbt.putString("id", id));
        }
        return nbt;
    }

    private static double[] entityPosition(CompoundTag entity) {
        var pos = entity.getList("pos").or(() -> entity.getList("Pos")).orElse(null);
        if (pos != null && pos.size() >= 3) {
            return new double[] {
                    pos.getDoubleOr(0, 0.0),
                    pos.getDoubleOr(1, 0.0),
                    pos.getDoubleOr(2, 0.0)
            };
        }
        return new double[] {
                entity.getDoubleOr("x", 0.0),
                entity.getDoubleOr("y", 0.0),
                entity.getDoubleOr("z", 0.0)
        };
    }

    private static ListTag spongeEntities(ListTag vanillaEntities) {
        var entities = new ListTag();
        vanillaEntities.compoundStream().forEach(entity -> entity.getCompound("nbt").ifPresent(nbt -> {
            var spongeEntity = nbt.copy();
            var pos = entityPosition(entity);
            if (!spongeEntity.contains("Pos")) {
                spongeEntity.put("Pos", doubleList(pos[0], pos[1], pos[2]));
            }
            if (!spongeEntity.contains("Id")) {
                nbt.getString("id").ifPresent(id -> spongeEntity.putString("Id", id));
            }
            entities.add(spongeEntity);
        }));
        return entities;
    }

    private static void copySchematicMetadata(CompoundTag source, CompoundTag target) {
        for (var key : List.of(
                "Metadata",
                "metadata",
                "Offset",
                "WEOffsetX",
                "WEOffsetY",
                "WEOffsetZ",
                "WEOriginX",
                "WEOriginY",
                "WEOriginZ")) {
            copyTag(source, target, key);
        }
    }

    private static void writeZeroOffsetDefaults(CompoundTag sponge) {
        if (!sponge.contains("Offset")) {
            sponge.putIntArray("Offset", new int[] { 0, 0, 0 });
        }
        for (var key : List.of("WEOffsetX", "WEOffsetY", "WEOffsetZ", "WEOriginX", "WEOriginY", "WEOriginZ")) {
            if (!sponge.contains(key)) {
                sponge.putInt(key, 0);
            }
        }
    }

    private static void copyTag(CompoundTag source, CompoundTag target, String key) {
        var tag = source.get(key);
        if (tag != null) {
            target.put(key, tag.copy());
        }
    }

    private static void requirePositiveSize(int width, int height, int length) throws IOException {
        if (width < 1 || height < 1 || length < 1) {
            throw new IOException("Structure dimensions must be positive");
        }
    }

    private static int[] decodeVarInts(byte[] data, int expected) throws IOException {
        var result = new int[expected];
        int value = 0;
        int bits = 0;
        int index = 0;
        for (byte datum : data) {
            value |= (datum & 0x7F) << bits;
            if ((datum & 0x80) == 0) {
                if (index >= expected) {
                    throw new IOException("Schematic BlockData has too many entries");
                }
                result[index++] = value;
                value = 0;
                bits = 0;
            } else {
                bits += 7;
                if (bits > 35) {
                    throw new IOException("Invalid schematic BlockData varint");
                }
            }
        }
        if (index != expected) {
            throw new IOException("Schematic BlockData has " + index + " entries, expected " + expected);
        }
        return result;
    }

    private static byte[] encodeVarInts(int[] values) {
        var output = new ByteArrayOutputStream();
        for (int value : values) {
            int remaining = value;
            while ((remaining & ~0x7F) != 0) {
                output.write((remaining & 0x7F) | 0x80);
                remaining >>>= 7;
            }
            output.write(remaining);
        }
        return output.toByteArray();
    }

    private static ListTag doubleList(double... values) {
        var list = new ListTag();
        for (double value : values) {
            list.add(DoubleTag.valueOf(value));
        }
        return list;
    }

    private static ListTag intList(int... values) {
        var list = new ListTag();
        for (int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    private static Rotation rotation(StructureRotation rotation) {
        return switch (rotation) {
            case NONE -> Rotation.NONE;
            case CLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            case CLOCKWISE_180 -> Rotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
        };
    }

    private static Mirror mirror(StructureMirror mirror) {
        return switch (mirror) {
            case NONE -> Mirror.NONE;
            case LEFT_RIGHT -> Mirror.LEFT_RIGHT;
            case FRONT_BACK -> Mirror.FRONT_BACK;
        };
    }

    private static ServerLevel level(io.fand.api.world.World world) {
        if (world instanceof FandWorld fandWorld) {
            return fandWorld.handle();
        }
        throw new IllegalArgumentException("World is not owned by this server: " + world.key().asString());
    }

    private static <T> CompletableFuture<T> notAttached() {
        return CompletableFuture.failedFuture(new IllegalStateException("Minecraft server is not attached"));
    }

    private static <T> CompletableFuture<T> submit(MinecraftServer server, Supplier<T> task) {
        if (server.isSameThread()) {
            return CompletableFuture.completedFuture(task.get());
        }
        return server.submit(task::get);
    }

    private static <T> T callOnServerThread(MinecraftServer server, Supplier<T> task) {
        if (server.isSameThread()) {
            return task.get();
        }
        return server.submit(task::get).join();
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }
}
