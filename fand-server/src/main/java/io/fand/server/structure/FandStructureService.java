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
import net.minecraft.world.level.block.state.BlockState;
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
        vanilla.put("blocks", indexedBlocks(width, height, length, states));
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
        if (blocks.length != width * height * length) {
            throw new IOException("Legacy schematic block array size mismatch");
        }
        var palette = new ListTag();
        var indices = new int[blocks.length];
        var stateIds = new java.util.LinkedHashMap<String, Integer>();
        for (int i = 0; i < blocks.length; i++) {
            int legacyId = blocks[i] & 0xFF;
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
        vanilla.put("blocks", indexedBlocks(width, height, length, indices));
        copyEntities(schematic, vanilla);
        return vanilla;
    }

    private static CompoundTag vanillaToSponge(CompoundTag vanilla) {
        var sponge = new CompoundTag();
        var size = vanilla.getListOrEmpty("size");
        int width = size.getIntOr(0, 0);
        int height = size.getIntOr(1, 0);
        int length = size.getIntOr(2, 0);
        sponge.putInt("Version", 2);
        sponge.putInt("DataVersion", vanilla.getIntOr("DataVersion", net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version()));
        sponge.putInt("Width", width);
        sponge.putInt("Height", height);
        sponge.putInt("Length", length);

        var palette = new CompoundTag();
        var paletteList = vanilla.getListOrEmpty("palette");
        for (int i = 0; i < paletteList.size(); i++) {
            palette.putInt(blockStateString(paletteList.getCompoundOrEmpty(i)), i);
        }
        sponge.put("Palette", palette);
        sponge.putInt("PaletteMax", paletteList.size());

        var states = new int[Math.max(0, width * height * length)];
        vanilla.getListOrEmpty("blocks").compoundStream().forEach(block -> {
            var pos = block.getListOrEmpty("pos");
            int index = blockIndex(width, length, pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0));
            if (index >= 0 && index < states.length) {
                states[index] = block.getIntOr("state", 0);
            }
        });
        sponge.putByteArray("BlockData", encodeVarInts(states));
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

    private static ListTag indexedBlocks(int width, int height, int length, int[] states) throws IOException {
        if (states.length != width * height * length) {
            throw new IOException("Block data length does not match schematic dimensions");
        }
        var blocks = new ListTag();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int state = states[blockIndex(width, length, x, y, z)];
                    if (isAirState(state)) {
                        continue;
                    }
                    var block = new CompoundTag();
                    block.put("pos", intList(x, y, z));
                    block.putInt("state", state);
                    blocks.add(block);
                }
            }
        }
        return blocks;
    }

    private static boolean isAirState(int stateId) {
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(stateId);
        return state == null || state.isAir();
    }

    private static int blockIndex(int width, int length, int x, int y, int z) {
        return (y * length + z) * width + x;
    }

    private static CompoundTag newBaseTemplate(int width, int height, int length) {
        var tag = new CompoundTag();
        tag.put("size", intList(width, height, length));
        tag.put("entities", new ListTag());
        tag.putInt("DataVersion", net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version());
        return tag;
    }

    private static void copyEntities(CompoundTag source, CompoundTag target) {
        var entities = source.getList("entities")
                .or(() -> source.getList("Entities"))
                .orElseGet(ListTag::new);
        target.put("entities", entities.copy());
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
