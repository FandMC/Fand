package io.fand.server.structure;

import com.mojang.datafixers.util.Pair;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fand.api.registry.RegistryReference;
import io.fand.api.structure.CustomStructure;
import io.fand.api.structure.CustomStructureSet;
import io.fand.api.structure.StructureFormat;
import io.fand.api.structure.StructureGenerationPlacement;
import io.fand.api.structure.StructureHeightPlacement;
import io.fand.api.structure.StructureHeightmap;
import io.fand.api.structure.StructureMirror;
import io.fand.api.structure.StructurePlacement;
import io.fand.api.structure.StructureProjection;
import io.fand.api.structure.StructureRegistration;
import io.fand.api.structure.StructureRotation;
import io.fand.api.structure.StructureService;
import io.fand.api.structure.StructureSetEntry;
import io.fand.api.structure.StructureSpreadType;
import io.fand.api.structure.StructureTerrainAdjustment;
import io.fand.api.structure.StructureTemplate;
import io.fand.api.structure.StructureVolume;
import io.fand.api.world.Location;
import io.fand.api.world.generation.DecorationStep;
import io.fand.server.world.FandWorld;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.structures.FandTemplateStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;

public final class FandStructureService implements StructureService {

    private final Supplier<MinecraftServer> server;
    private final Object lock = new Object();
    private final LinkedHashMap<Key, RegisteredStructure> structures = new LinkedHashMap<>();
    private final LinkedHashMap<Key, RegisteredStructureSet> structureSets = new LinkedHashMap<>();
    private final java.util.Set<Key> runtimeStructureKeys = new HashSet<>();
    private final java.util.Set<Key> runtimeStructureSetKeys = new HashSet<>();
    private final AtomicLong sequence = new AtomicLong();

    public FandStructureService(Supplier<MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public Collection<CustomStructure> registeredStructures() {
        synchronized (lock) {
            return structures.values().stream()
                    .map(RegisteredStructure::definition)
                    .toList();
        }
    }

    @Override
    public Optional<CustomStructure> registeredStructure(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return Optional.ofNullable(structures.get(key)).map(RegisteredStructure::definition);
        }
    }

    @Override
    public StructureRegistration registerStructure(CustomStructure structure) {
        Objects.requireNonNull(structure, "structure");
        long token = sequence.incrementAndGet();
        synchronized (lock) {
            runtimeStructureKeys.add(structure.key());
            structures.put(structure.key(), new RegisteredStructure(structure, token));
        }
        applyStructure(structure);
        return new Registration(this, structure.key(), token, RegistrationKind.STRUCTURE);
    }

    @Override
    public Collection<CustomStructureSet> registeredStructureSets() {
        synchronized (lock) {
            return structureSets.values().stream()
                    .map(RegisteredStructureSet::definition)
                    .toList();
        }
    }

    @Override
    public Optional<CustomStructureSet> registeredStructureSet(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return Optional.ofNullable(structureSets.get(key)).map(RegisteredStructureSet::definition);
        }
    }

    @Override
    public StructureRegistration registerStructureSet(CustomStructureSet structureSet) {
        Objects.requireNonNull(structureSet, "structureSet");
        long token = sequence.incrementAndGet();
        synchronized (lock) {
            runtimeStructureSetKeys.add(structureSet.key());
            structureSets.put(structureSet.key(), new RegisteredStructureSet(structureSet, token));
        }
        applyStructureSet(structureSet);
        return new Registration(this, structureSet.key(), token, RegistrationKind.STRUCTURE_SET);
    }

    public void applyLoadedStructures() {
        var current = server.get();
        if (current == null) {
            return;
        }
        registeredStructures().forEach(structure -> registerVanillaStructure(current, structure));
        registeredStructureSets().forEach(structureSet -> registerVanillaStructureSet(current, structureSet));
        refreshStructureStates(current);
    }

    public java.util.stream.Stream<Holder.Reference<StructureSet>> structureSetHolders() {
        var current = server.get();
        if (current == null) {
            return java.util.stream.Stream.empty();
        }
        var registry = current.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        return registeredStructureSets().stream()
                .map(CustomStructureSet::key)
                .map(key -> registry.get(ResourceKey.create(Registries.STRUCTURE_SET, identifier(key))))
                .flatMap(Optional::stream);
    }

    public boolean runtimeStructureSetActive(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return !runtimeStructureSetKeys.contains(key) || structureSets.containsKey(key);
        }
    }

    public boolean runtimeStructureSetOwned(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return runtimeStructureSetKeys.contains(key);
        }
    }

    public boolean runtimeStructureActive(Key key) {
        synchronized (lock) {
            return !runtimeStructureKeys.contains(key) || structures.containsKey(key);
        }
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

    public CompletableFuture<Boolean> placeEphemeral(StructureProjection projection, Location origin, StructurePlacement placement) {
        Objects.requireNonNull(projection, "projection");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(placement, "placement");
        var current = server.get();
        if (current == null) {
            return notAttached();
        }
        return submit(current, () -> placeEphemeralOnServerThread(current, projection, origin, placement));
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

    private boolean placeEphemeralOnServerThread(
            MinecraftServer server,
            StructureProjection projection,
            Location origin,
            StructurePlacement placement
    ) {
        var key = Key.key("fand", "projection/" + UUID.randomUUID());
        var id = identifier(key);
        try {
            return importOnServerThread(server, key, projection)
                    && placeOnServerThread(key, origin, placement);
        } finally {
            server.getStructureManager().remove(id);
        }
    }

    private Optional<Location> locateOnServerThread(Key structure, Location origin, int radius) {
        if (!runtimeStructureActive(structure)) {
            return Optional.empty();
        }
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

    private void applyStructure(CustomStructure structure) {
        var current = server.get();
        if (current == null) {
            return;
        }
        callOnServerThread(current, () -> {
            registerVanillaStructure(current, structure);
            refreshStructureStates(current);
            return null;
        });
    }

    private void applyStructureSet(CustomStructureSet structureSet) {
        var current = server.get();
        if (current == null) {
            return;
        }
        callOnServerThread(current, () -> {
            registerVanillaStructureSet(current, structureSet);
            refreshStructureStates(current);
            return null;
        });
    }

    private static void refreshStructureStates(MinecraftServer server) {
        for (var level : server.getAllLevels()) {
            level.getChunkSource().fand$refreshGeneratorState();
        }
    }

    private static void registerVanillaStructure(MinecraftServer server, CustomStructure structure) {
        var registry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        if (!(registry instanceof MappedRegistry<Structure> mapped)) {
            throw new IllegalStateException("Structure registry is not writable: " + registry);
        }
        mapped.fand$registerRuntime(
                ResourceKey.create(Registries.STRUCTURE, identifier(structure.key())),
                vanillaStructure(server, structure),
                RegistrationInfo.BUILT_IN);
    }

    private static void registerVanillaStructureSet(MinecraftServer server, CustomStructureSet structureSet) {
        var registry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        if (!(registry instanceof MappedRegistry<StructureSet> mapped)) {
            throw new IllegalStateException("Structure set registry is not writable: " + registry);
        }
        mapped.fand$registerRuntime(
                ResourceKey.create(Registries.STRUCTURE_SET, identifier(structureSet.key())),
                vanillaStructureSet(server, structureSet),
                RegistrationInfo.BUILT_IN);
    }

    private static Structure vanillaStructure(MinecraftServer server, CustomStructure structure) {
        return new FandTemplateStructure(
                new Structure.StructureSettings.Builder(biomeSet(server, structure.biomes()))
                        .generationStep(step(structure.step()))
                        .terrainAdapation(terrainAdjustment(structure.terrainAdjustment()))
                        .build(),
                identifier(structure.template()),
                structure.includeEntities(),
                heightmap(structure.heightPlacement()),
                structure.heightPlacement().offset());
    }

    private static StructureSet vanillaStructureSet(MinecraftServer server, CustomStructureSet structureSet) {
        var structures = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var entries = structureSet.structures().stream()
                .map(entry -> StructureSet.entry(structureHolder(structures, entry), entry.weight()))
                .toList();
        return new StructureSet(entries, placement(structureSet.placement()));
    }

    private static Holder<Structure> structureHolder(net.minecraft.core.Registry<Structure> registry, StructureSetEntry entry) {
        return registry.get(ResourceKey.create(Registries.STRUCTURE, identifier(entry.structure())))
                .orElseThrow(() -> new IllegalArgumentException("Unknown structure: " + entry.structure().asString()));
    }

    private static HolderSet<Biome> biomeSet(MinecraftServer server, List<RegistryReference> references) {
        var biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
        var holders = new LinkedHashMap<Identifier, Holder<Biome>>();
        for (var reference : references) {
            if (reference.tag() && reference.key().equals(Key.key("fand:all"))) {
                biomes.listElements().forEach(holder -> holders.put(holder.key().identifier(), holder));
                continue;
            }
            if (reference.tag()) {
                var tag = TagKey.create(Registries.BIOME, identifier(reference.key()));
                var named = biomes.get(tag)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown biome tag: " + reference.asString()));
                named.stream().forEach(holder -> holders.put(holder.unwrapKey().orElseThrow().identifier(), holder));
            } else {
                var holder = biomes.get(ResourceKey.create(Registries.BIOME, identifier(reference.key())))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown biome: " + reference.asString()));
                holders.put(holder.key().identifier(), holder);
            }
        }
        if (holders.isEmpty()) {
            throw new IllegalArgumentException("Structure biome set resolved to empty: " + references);
        }
        return HolderSet.direct(holders.values().stream().toList());
    }

    private static net.minecraft.world.level.levelgen.structure.placement.StructurePlacement placement(StructureGenerationPlacement placement) {
        return new RandomSpreadStructurePlacement(
                Vec3i.ZERO,
                net.minecraft.world.level.levelgen.structure.placement.StructurePlacement.FrequencyReductionMethod.DEFAULT,
                placement.frequency(),
                placement.salt(),
                Optional.empty(),
                placement.spacing(),
                placement.separation(),
                spreadType(placement.spreadType()));
    }

    private static RandomSpreadType spreadType(StructureSpreadType spreadType) {
        return switch (spreadType) {
            case LINEAR -> RandomSpreadType.LINEAR;
            case TRIANGULAR -> RandomSpreadType.TRIANGULAR;
        };
    }

    private static GenerationStep.Decoration step(DecorationStep step) {
        return switch (step) {
            case RAW_GENERATION -> GenerationStep.Decoration.RAW_GENERATION;
            case LAKES -> GenerationStep.Decoration.LAKES;
            case LOCAL_MODIFICATIONS -> GenerationStep.Decoration.LOCAL_MODIFICATIONS;
            case UNDERGROUND_STRUCTURES -> GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
            case SURFACE_STRUCTURES -> GenerationStep.Decoration.SURFACE_STRUCTURES;
            case STRONGHOLDS -> GenerationStep.Decoration.STRONGHOLDS;
            case UNDERGROUND_ORES -> GenerationStep.Decoration.UNDERGROUND_ORES;
            case UNDERGROUND_DECORATION -> GenerationStep.Decoration.UNDERGROUND_DECORATION;
            case FLUID_SPRINGS -> GenerationStep.Decoration.FLUID_SPRINGS;
            case VEGETAL_DECORATION -> GenerationStep.Decoration.VEGETAL_DECORATION;
            case TOP_LAYER_MODIFICATION -> GenerationStep.Decoration.TOP_LAYER_MODIFICATION;
        };
    }

    private static TerrainAdjustment terrainAdjustment(StructureTerrainAdjustment adjustment) {
        return switch (adjustment) {
            case NONE -> TerrainAdjustment.NONE;
            case BURY -> TerrainAdjustment.BURY;
            case BEARD_THIN -> TerrainAdjustment.BEARD_THIN;
            case BEARD_BOX -> TerrainAdjustment.BEARD_BOX;
            case ENCAPSULATE -> TerrainAdjustment.ENCAPSULATE;
        };
    }

    private static Heightmap.Types heightmap(StructureHeightPlacement placement) {
        return heightmap(placement.heightmap());
    }

    private static Heightmap.Types heightmap(StructureHeightmap heightmap) {
        return switch (heightmap) {
            case WORLD_SURFACE_WG -> Heightmap.Types.WORLD_SURFACE_WG;
            case WORLD_SURFACE -> Heightmap.Types.WORLD_SURFACE;
            case OCEAN_FLOOR_WG -> Heightmap.Types.OCEAN_FLOOR_WG;
            case OCEAN_FLOOR -> Heightmap.Types.OCEAN_FLOOR;
            case MOTION_BLOCKING -> Heightmap.Types.MOTION_BLOCKING;
            case MOTION_BLOCKING_NO_LEAVES -> Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        };
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
            case BLU -> writeBlu(tag);
            case LITEMATIC -> writeLitematic(tag);
        };
    }

    private static CompoundTag readTag(byte[] data, StructureFormat format) throws IOException {
        return switch (format) {
            case VANILLA_NBT -> NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
            case VANILLA_SNBT -> snbtToStructure(data);
            case SPONGE_SCHEMATIC, WORLDEDIT_SCHEMATIC -> schematicToVanilla(NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap()));
            case BLU -> bluToVanilla(data);
            case LITEMATIC -> litematicToVanilla(NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap()));
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
        if (name.endsWith(".blu")) {
            return StructureFormat.BLU;
        }
        if (name.endsWith(".litematic")) {
            return StructureFormat.LITEMATIC;
        }
        return StructureFormat.VANILLA_NBT;
    }

    private static StructureFormat detectFormat(byte[] data) {
        if (isBlu(data)) {
            return StructureFormat.BLU;
        }
        if (data.length > 0 && (data[0] == '{' || data[0] == '[')) {
            return StructureFormat.VANILLA_SNBT;
        }
        try {
            var tag = NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
            if (isLitematic(tag)) {
                return StructureFormat.LITEMATIC;
            }
            if (tag.contains("Palette") || tag.contains("BlockData") || tag.contains("Blocks")) {
                return tag.contains("Palette") ? StructureFormat.SPONGE_SCHEMATIC : StructureFormat.WORLDEDIT_SCHEMATIC;
            }
        } catch (IOException ignored) {
        }
        return StructureFormat.VANILLA_NBT;
    }

    private static byte[] writeBlu(CompoundTag tag) throws IOException {
        var json = vanillaToBlu(tag);
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(json.get("name").getAsString()));
            zip.write(json.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static CompoundTag bluToVanilla(byte[] data) throws IOException {
        return bluJsonToVanilla(readBluJson(data));
    }

    private static boolean isBlu(byte[] data) {
        if (data.length < 4 || data[0] != 'P' || data[1] != 'K') {
            return false;
        }
        try {
            try (var zip = new ZipInputStream(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        zip.closeEntry();
                        continue;
                    }
                    var text = new String(zip.readAllBytes(), StandardCharsets.UTF_8).trim();
                    zip.closeEntry();
                    if (text.startsWith("{")
                            && text.contains("\"xSize\"")
                            && text.contains("\"ySize\"")
                            && text.contains("\"zSize\"")) {
                        return true;
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return false;
    }

    private static JsonObject readBluJson(byte[] data) throws IOException {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zip.closeEntry();
                    continue;
                }
                var raw = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                zip.closeEntry();
                var json = parseJson(raw);
                if (json.isPresent() && json.orElseThrow().isJsonObject()) {
                    var object = json.orElseThrow().getAsJsonObject();
                    if (object.has("xSize") && object.has("ySize") && object.has("zSize")) {
                        return object;
                    }
                }
            }
        } catch (RuntimeException failure) {
            throw new IOException("Invalid BLU blueprint JSON", failure);
        }
        throw new IOException("BLU archive does not contain a blueprint entry");
    }

    private static Optional<JsonElement> parseJson(String raw) {
        try {
            return Optional.of(JsonParser.parseString(raw));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static CompoundTag bluJsonToVanilla(JsonObject json) throws IOException {
        int width = bluInt(json, "xSize");
        int height = bluInt(json, "ySize");
        int length = bluInt(json, "zSize");
        requirePositiveSize(width, height, length);

        var rawBlocks = new java.util.ArrayList<BluBlock>();
        addBluBlocks(json.get("blocks"), rawBlocks);
        addBluBlocks(json.get("attached"), rawBlocks);
        bluVector(json.get("bedrock")).ifPresent(pos -> rawBlocks.add(new BluBlock(pos[0], pos[1], pos[2], "minecraft:bedrock")));
        if (rawBlocks.isEmpty()) {
            rawBlocks.add(new BluBlock(width / 2, height / 2, length / 2, "minecraft:bedrock"));
        }

        boolean centered = rawBlocks.stream().anyMatch(block -> block.x() < 0 || block.y() < 0 || block.z() < 0);
        var placedBlocks = new LinkedHashMap<String, BluBlock>();
        for (var rawBlock : rawBlocks) {
            int x = centered ? rawBlock.x() + width / 2 : rawBlock.x();
            int y = centered ? rawBlock.y() + height / 2 : rawBlock.y();
            int z = centered ? rawBlock.z() + length / 2 : rawBlock.z();
            if (insideVolume(x, y, z, width, height, length)) {
                placedBlocks.put(x + "," + y + "," + z, new BluBlock(x, y, z, rawBlock.state()));
            }
        }

        var vanilla = newBaseTemplate(width, height, length);
        var paletteIds = new LinkedHashMap<String, Integer>();
        var palette = new ListTag();
        var blocks = new ListTag();
        for (var placedBlock : placedBlocks.values()) {
            var state = normalizeBluBlockState(placedBlock.state());
            if (state.equals("minecraft:air")) {
                continue;
            }
            Integer stateId = paletteIds.get(state);
            if (stateId == null) {
                stateId = paletteIds.size();
                paletteIds.put(state, stateId);
                palette.add(blockStateTag(state));
            }
            var block = new CompoundTag();
            block.put("pos", intList(placedBlock.x(), placedBlock.y(), placedBlock.z()));
            block.putInt("state", stateId);
            blocks.add(block);
        }
        if (palette.isEmpty()) {
            palette.add(blockStateTag("minecraft:air"));
        }
        vanilla.put("palette", palette);
        vanilla.put("blocks", blocks);
        return vanilla;
    }

    private static JsonObject vanillaToBlu(CompoundTag vanilla) {
        var size = vanilla.getListOrEmpty("size");
        int width = size.getIntOr(0, 1);
        int height = size.getIntOr(1, 1);
        int length = size.getIntOr(2, 1);
        var json = new JsonObject();
        json.addProperty("name", "structure");
        json.addProperty("xSize", width);
        json.addProperty("ySize", height);
        json.addProperty("zSize", length);

        var palette = vanillaPalette(vanilla);
        var blocks = new JsonObject();
        JsonArray[] bedrock = { null };
        vanilla.getListOrEmpty("blocks").compoundStream().forEach(block -> {
            var pos = block.getListOrEmpty("pos");
            int stateId = block.getIntOr("state", 0);
            var state = stateId >= 0 && stateId < palette.size()
                    ? blockStateString(palette.getCompoundOrEmpty(stateId))
                    : "minecraft:air";
            if (state.equals("minecraft:air")) {
                return;
            }
            var vector = bluVector(pos.getIntOr(0, 0) - width / 2, pos.getIntOr(1, 0) - height / 2, pos.getIntOr(2, 0) - length / 2);
            var blockJson = new JsonObject();
            blockJson.addProperty("blockData", state);
            if ("minecraft:bedrock".equals(blockJson.get("blockData").getAsString())) {
                bedrock[0] = vector;
            }
            blocks.add(vector.toString(), blockJson);
        });
        json.add("blocks", blocks);
        json.add("bedrock", bedrock[0] == null ? bluVector(0, 0, 0) : bedrock[0]);
        return json;
    }

    private static int bluInt(JsonObject json, String key) throws IOException {
        var value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IOException("BLU blueprint missing numeric " + key);
        }
        return value.getAsInt();
    }

    private static void addBluBlocks(JsonElement element, java.util.List<BluBlock> output) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            for (var entry : element.getAsJsonObject().entrySet()) {
                bluVector(entry.getKey()).ifPresent(pos -> output.add(new BluBlock(pos[0], pos[1], pos[2], bluBlockState(entry.getValue()))));
            }
            return;
        }
        if (!element.isJsonArray()) {
            return;
        }
        for (var rawEntry : element.getAsJsonArray()) {
            if (rawEntry.isJsonArray()) {
                var pair = rawEntry.getAsJsonArray();
                if (pair.size() >= 2) {
                    bluVector(pair.get(0)).ifPresent(pos -> output.add(new BluBlock(pos[0], pos[1], pos[2], bluBlockState(pair.get(1)))));
                }
            } else if (rawEntry.isJsonObject()) {
                var object = rawEntry.getAsJsonObject();
                var position = object.has("pos") ? object.get("pos") : object.get("position");
                var block = object.has("block") ? object.get("block") : object;
                bluVector(position).ifPresent(pos -> output.add(new BluBlock(pos[0], pos[1], pos[2], bluBlockState(block))));
            }
        }
    }

    private static String bluBlockState(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "minecraft:air";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (!element.isJsonObject()) {
            return "minecraft:air";
        }
        var object = element.getAsJsonObject();
        for (var key : List.of("blockData", "BlockData", "state", "id")) {
            var value = object.get(key);
            if (value != null && value.isJsonPrimitive()) {
                return value.getAsString();
            }
        }
        return "minecraft:air";
    }

    private static String normalizeBluBlockState(String state) {
        var trimmed = state == null ? "" : state.trim();
        if (trimmed.isEmpty()) {
            return "minecraft:air";
        }
        int propertyStart = trimmed.indexOf('[');
        var name = propertyStart >= 0 ? trimmed.substring(0, propertyStart) : trimmed;
        var properties = propertyStart >= 0 ? trimmed.substring(propertyStart) : "";
        if (!name.contains(":")) {
            name = "minecraft:" + name.toLowerCase(Locale.ROOT);
        }
        return name + properties;
    }

    private static Optional<int[]> bluVector(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            if (array.size() < 3) {
                return Optional.empty();
            }
            return Optional.of(new int[] { bluCoordinate(array.get(0)), bluCoordinate(array.get(1)), bluCoordinate(array.get(2)) });
        }
        if (element.isJsonPrimitive()) {
            return bluVector(element.getAsString());
        }
        if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            if (object.has("x") && object.has("y") && object.has("z")) {
                return Optional.of(new int[] { bluCoordinate(object.get("x")), bluCoordinate(object.get("y")), bluCoordinate(object.get("z")) });
            }
        }
        return Optional.empty();
    }

    private static Optional<int[]> bluVector(String key) {
        try {
            var parsed = JsonParser.parseString(key);
            if (parsed.isJsonArray()) {
                return bluVector(parsed);
            }
        } catch (RuntimeException ignored) {
        }
        var cleaned = key.replace('[', ' ')
                .replace(']', ' ')
                .replace('(', ' ')
                .replace(')', ' ')
                .replace(';', ',')
                .replace("Vector", "")
                .trim();
        cleaned = cleaned.replace("x=", "")
                .replace("y=", "")
                .replace("z=", "");
        var parts = cleaned.split(",");
        if (parts.length < 3) {
            parts = cleaned.split("\\s+");
        }
        if (parts.length < 3) {
            return Optional.empty();
        }
        return Optional.of(new int[] { bluCoordinate(parts[0]), bluCoordinate(parts[1]), bluCoordinate(parts[2]) });
    }

    private static int bluCoordinate(JsonElement element) {
        return (int) Math.round(element.getAsDouble());
    }

    private static int bluCoordinate(String value) {
        var trimmed = value.trim();
        int split = trimmed.indexOf('=');
        if (split >= 0) {
            trimmed = trimmed.substring(split + 1);
        }
        return (int) Math.round(Double.parseDouble(trimmed));
    }

    private static JsonArray bluVector(int x, int y, int z) {
        var vector = new JsonArray();
        vector.add(x);
        vector.add(y);
        vector.add(z);
        return vector;
    }

    private static byte[] writeLitematic(CompoundTag vanilla) throws IOException {
        var root = vanillaToLitematic(vanilla);
        var out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, out);
        return out.toByteArray();
    }

    private static boolean isLitematic(CompoundTag tag) {
        return tag.contains("Regions")
                && (tag.contains("Metadata") || tag.contains("Version") || tag.contains("MinecraftDataVersion"));
    }

    private static CompoundTag litematicToVanilla(CompoundTag litematic) throws IOException {
        var regions = litematic.getCompoundOrEmpty("Regions");
        var parsed = new java.util.ArrayList<LitematicRegion>();
        for (var entry : regions.entrySet()) {
            if (entry.getValue() instanceof CompoundTag region) {
                parsed.add(readLitematicRegion(region));
            }
        }
        if (parsed.isEmpty()) {
            throw new IOException("Litematic contains no regions");
        }

        int minX = parsed.stream().mapToInt(LitematicRegion::minX).min().orElse(0);
        int minY = parsed.stream().mapToInt(LitematicRegion::minY).min().orElse(0);
        int minZ = parsed.stream().mapToInt(LitematicRegion::minZ).min().orElse(0);
        int maxX = parsed.stream().mapToInt(LitematicRegion::maxX).max().orElse(0);
        int maxY = parsed.stream().mapToInt(LitematicRegion::maxY).max().orElse(0);
        int maxZ = parsed.stream().mapToInt(LitematicRegion::maxZ).max().orElse(0);
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int length = maxZ - minZ + 1;
        requirePositiveSize(width, height, length);

        var vanilla = newBaseTemplate(width, height, length);
        var paletteIds = new LinkedHashMap<String, Integer>();
        var palette = new ListTag();
        var blocks = new ListTag();
        var blockEntityByPosition = new java.util.HashMap<String, CompoundTag>();
        parsed.forEach(region -> collectLitematicBlockEntities(region, minX, minY, minZ, blockEntityByPosition));

        for (var region : parsed) {
            int[] states = decodePackedLongArray(region.blockStates(), region.palette().size(), region.volume());
            for (int y = 0; y < region.height(); y++) {
                for (int z = 0; z < region.length(); z++) {
                    for (int x = 0; x < region.width(); x++) {
                        int localIndex = blockIndex(region.width(), region.length(), x, y, z);
                        int paletteIndex = states[localIndex];
                        if (paletteIndex < 0 || paletteIndex >= region.palette().size()) {
                            throw new IOException("Litematic block state index outside palette");
                        }
                        var state = blockStateString(region.palette().getCompoundOrEmpty(paletteIndex));
                        Integer stateId = paletteIds.get(state);
                        if (stateId == null) {
                            stateId = paletteIds.size();
                            paletteIds.put(state, stateId);
                            palette.add(blockStateTag(state));
                        }

                        int globalX = region.minX() + x;
                        int globalY = region.minY() + y;
                        int globalZ = region.minZ() + z;
                        int targetX = globalX - minX;
                        int targetY = globalY - minY;
                        int targetZ = globalZ - minZ;
                        var block = new CompoundTag();
                        block.put("pos", intList(targetX, targetY, targetZ));
                        block.putInt("state", stateId);
                        var blockEntity = blockEntityByPosition.get(positionKey(targetX, targetY, targetZ));
                        if (blockEntity != null) {
                            block.put("nbt", blockEntity);
                        }
                        blocks.add(block);
                    }
                }
            }
        }

        vanilla.put("palette", palette);
        vanilla.put("blocks", blocks);
        vanilla.put("entities", litematicEntities(parsed, minX, minY, minZ));
        litematic.getCompound("Metadata").ifPresent(metadata -> vanilla.put("Metadata", metadata.copy()));
        vanilla.putInt("DataVersion", litematic.getIntOr("MinecraftDataVersion", currentDataVersion()));
        return vanilla;
    }

    private static LitematicRegion readLitematicRegion(CompoundTag region) throws IOException {
        int[] position = litematicVector(region.getCompoundOrEmpty("Position"));
        int[] rawSize = litematicVector(region.getCompoundOrEmpty("Size"));
        int width = Math.abs(rawSize[0]);
        int height = Math.abs(rawSize[1]);
        int length = Math.abs(rawSize[2]);
        requirePositiveSize(width, height, length);

        int minX = rawSize[0] < 0 ? position[0] + rawSize[0] + 1 : position[0];
        int minY = rawSize[1] < 0 ? position[1] + rawSize[1] + 1 : position[1];
        int minZ = rawSize[2] < 0 ? position[2] + rawSize[2] + 1 : position[2];
        var palette = region.getListOrEmpty("BlockStatePalette");
        if (palette.isEmpty()) {
            throw new IOException("Litematic region missing BlockStatePalette");
        }
        var blockStates = region.getLongArray("BlockStates").orElse(new long[0]);
        return new LitematicRegion(
                minX,
                minY,
                minZ,
                width,
                height,
                length,
                palette,
                blockStates,
                region.getListOrEmpty("TileEntities"),
                region.getListOrEmpty("Entities"));
    }

    private static CompoundTag vanillaToLitematic(CompoundTag vanilla) {
        var size = vanilla.getListOrEmpty("size");
        int width = size.getIntOr(0, 1);
        int height = size.getIntOr(1, 1);
        int length = size.getIntOr(2, 1);
        var palette = copyPaletteWithAir(vanillaPalette(vanilla));
        int airState = findPaletteIndex(palette, "minecraft:air");
        int[] states = new int[width * height * length];
        java.util.Arrays.fill(states, Math.max(airState, 0));
        var tileEntities = new ListTag();
        vanilla.getListOrEmpty("blocks").compoundStream().forEach(block -> {
            var pos = block.getListOrEmpty("pos");
            int x = pos.getIntOr(0, 0);
            int y = pos.getIntOr(1, 0);
            int z = pos.getIntOr(2, 0);
            if (!insideVolume(x, y, z, width, height, length)) {
                return;
            }
            states[blockIndex(width, length, x, y, z)] = block.getIntOr("state", Math.max(airState, 0));
            block.getCompound("nbt").ifPresent(nbt -> {
                var blockEntity = nbt.copy();
                blockEntity.putInt("x", x);
                blockEntity.putInt("y", y);
                blockEntity.putInt("z", z);
                tileEntities.add(blockEntity);
            });
        });

        var region = new CompoundTag();
        region.put("Position", litematicVectorTag(0, 0, 0));
        region.put("Size", litematicVectorTag(width, height, length));
        region.put("BlockStatePalette", palette);
        region.putLongArray("BlockStates", encodePackedLongArray(states, palette.size()));
        if (!tileEntities.isEmpty()) {
            region.put("TileEntities", tileEntities);
        }
        var entities = litematicEntityList(vanilla.getListOrEmpty("entities"));
        if (!entities.isEmpty()) {
            region.put("Entities", entities);
        }

        var regions = new CompoundTag();
        regions.put("main", region);
        var root = new CompoundTag();
        root.putInt("Version", 7);
        root.putInt("MinecraftDataVersion", currentDataVersion());
        root.put("Metadata", litematicMetadata(vanilla, width, height, length));
        root.put("Regions", regions);
        return root;
    }

    private static CompoundTag litematicMetadata(CompoundTag vanilla, int width, int height, int length) {
        var metadata = vanilla.getCompoundOrEmpty("Metadata").copy();
        if (!metadata.contains("Name")) {
            metadata.putString("Name", "structure");
        }
        if (!metadata.contains("Author")) {
            metadata.putString("Author", "Fand");
        }
        metadata.put("EnclosingSize", litematicVectorTag(width, height, length));
        metadata.putInt("RegionCount", 1);
        metadata.putInt("TotalVolume", width * height * length);
        metadata.putLong("TimeModified", System.currentTimeMillis());
        if (!metadata.contains("TimeCreated")) {
            metadata.putLong("TimeCreated", System.currentTimeMillis());
        }
        return metadata;
    }

    private static ListTag copyPaletteWithAir(ListTag source) {
        var palette = new ListTag();
        boolean hasAir = false;
        for (int i = 0; i < source.size(); i++) {
            var state = source.getCompoundOrEmpty(i).copy();
            hasAir |= "minecraft:air".equals(blockStateString(state));
            palette.add(state);
        }
        if (!hasAir) {
            palette.add(blockStateTag("minecraft:air"));
        }
        return palette;
    }

    private static int[] decodePackedLongArray(long[] packed, int paletteSize, int expected) throws IOException {
        int bits = packedBits(paletteSize);
        long mask = (1L << bits) - 1L;
        var states = new int[expected];
        if (packed.length == 0) {
            return states;
        }
        for (int i = 0; i < expected; i++) {
            long bitIndex = (long) i * bits;
            int longIndex = (int) (bitIndex >>> 6);
            int bitOffset = (int) (bitIndex & 63L);
            if (longIndex >= packed.length) {
                throw new IOException("Litematic BlockStates ended early");
            }
            long value = packed[longIndex] >>> bitOffset;
            int spill = bitOffset + bits - 64;
            if (spill > 0) {
                if (longIndex + 1 >= packed.length) {
                    throw new IOException("Litematic BlockStates ended mid-entry");
                }
                value |= packed[longIndex + 1] << (bits - spill);
            }
            states[i] = (int) (value & mask);
        }
        return states;
    }

    private static long[] encodePackedLongArray(int[] states, int paletteSize) {
        int bits = packedBits(paletteSize);
        long mask = (1L << bits) - 1L;
        int length = (int) ((((long) states.length * bits) + 63L) >>> 6);
        var packed = new long[Math.max(length, 1)];
        for (int i = 0; i < states.length; i++) {
            long value = states[i] & mask;
            long bitIndex = (long) i * bits;
            int longIndex = (int) (bitIndex >>> 6);
            int bitOffset = (int) (bitIndex & 63L);
            packed[longIndex] = (packed[longIndex] & ~(mask << bitOffset)) | (value << bitOffset);
            int spill = bitOffset + bits - 64;
            if (spill > 0) {
                long spillMask = (1L << spill) - 1L;
                packed[longIndex + 1] = (packed[longIndex + 1] & ~spillMask) | (value >>> (bits - spill));
            }
        }
        return packed;
    }

    private static int packedBits(int paletteSize) {
        return Math.max(1, 32 - Integer.numberOfLeadingZeros(Math.max(1, paletteSize - 1)));
    }

    private static void collectLitematicBlockEntities(
            LitematicRegion region,
            int originX,
            int originY,
            int originZ,
            java.util.Map<String, CompoundTag> target) {
        region.blockEntities().compoundStream().forEach(blockEntity -> {
            int[] pos = litematicStoredPosition(blockEntity, region);
            int x = pos[0] - originX;
            int y = pos[1] - originY;
            int z = pos[2] - originZ;
            var nbt = normalizeBlockEntityNbt(blockEntity);
            nbt.putInt("x", x);
            nbt.putInt("y", y);
            nbt.putInt("z", z);
            target.put(positionKey(x, y, z), nbt);
        });
    }

    private static ListTag litematicEntities(java.util.List<LitematicRegion> regions, int originX, int originY, int originZ) {
        var entities = new ListTag();
        regions.forEach(region -> region.entities().compoundStream().forEach(source -> {
            var nbt = normalizeEntityNbt(source);
            var pos = entityPosition(source);
            if (insideVolume((int) Math.floor(pos[0]), (int) Math.floor(pos[1]), (int) Math.floor(pos[2]), region.width(), region.height(), region.length())) {
                pos[0] += region.minX();
                pos[1] += region.minY();
                pos[2] += region.minZ();
            }
            var entity = new CompoundTag();
            entity.put("pos", doubleList(pos[0] - originX, pos[1] - originY, pos[2] - originZ));
            entity.put("blockPos", intList(
                    (int) Math.floor(pos[0] - originX),
                    (int) Math.floor(pos[1] - originY),
                    (int) Math.floor(pos[2] - originZ)));
            entity.put("nbt", nbt);
            entities.add(entity);
        }));
        return entities;
    }

    private static ListTag litematicEntityList(ListTag vanillaEntities) {
        var entities = new ListTag();
        vanillaEntities.compoundStream().forEach(entity -> {
            var nbt = entity.getCompound("nbt").map(CompoundTag::copy).orElseGet(entity::copy);
            var pos = entityPosition(entity);
            nbt.put("Pos", doubleList(pos[0], pos[1], pos[2]));
            entities.add(nbt);
        });
        return entities;
    }

    private static int[] litematicStoredPosition(CompoundTag tag, LitematicRegion region) {
        int[] pos = blockEntityPosition(tag);
        if (insideVolume(pos[0], pos[1], pos[2], region.width(), region.height(), region.length())) {
            return new int[] { region.minX() + pos[0], region.minY() + pos[1], region.minZ() + pos[2] };
        }
        return pos;
    }

    private static int[] litematicVector(CompoundTag tag) {
        return new int[] {
                tag.getIntOr("x", tag.getIntOr("X", 0)),
                tag.getIntOr("y", tag.getIntOr("Y", 0)),
                tag.getIntOr("z", tag.getIntOr("Z", 0))
        };
    }

    private static CompoundTag litematicVectorTag(int x, int y, int z) {
        var tag = new CompoundTag();
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        return tag;
    }

    private static String positionKey(int x, int y, int z) {
        return x + "," + y + "," + z;
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

    private boolean active(Key key, long token, RegistrationKind kind) {
        synchronized (lock) {
            return switch (kind) {
                case STRUCTURE -> {
                    var current = structures.get(key);
                    yield current != null && current.token() == token;
                }
                case STRUCTURE_SET -> {
                    var current = structureSets.get(key);
                    yield current != null && current.token() == token;
                }
            };
        }
    }

    private boolean unregister(Key key, long token, RegistrationKind kind) {
        synchronized (lock) {
            return switch (kind) {
                case STRUCTURE -> {
                    var current = structures.get(key);
                    if (current == null || current.token() != token) {
                        yield false;
                    }
                    structures.remove(key);
                    yield true;
                }
                case STRUCTURE_SET -> {
                    var current = structureSets.get(key);
                    if (current == null || current.token() != token) {
                        yield false;
                    }
                    structureSets.remove(key);
                    yield true;
                }
            };
        }
    }

    private record BluBlock(int x, int y, int z, String state) {
    }

    private record LitematicRegion(
            int minX,
            int minY,
            int minZ,
            int width,
            int height,
            int length,
            ListTag palette,
            long[] blockStates,
            ListTag blockEntities,
            ListTag entities) {

        int volume() {
            return width * height * length;
        }

        int maxX() {
            return minX + width - 1;
        }

        int maxY() {
            return minY + height - 1;
        }

        int maxZ() {
            return minZ + length - 1;
        }
    }

    private record RegisteredStructure(CustomStructure definition, long token) {
    }

    private record RegisteredStructureSet(CustomStructureSet definition, long token) {
    }

    private enum RegistrationKind {
        STRUCTURE,
        STRUCTURE_SET
    }

    private static final class Registration implements StructureRegistration {

        private final FandStructureService owner;
        private final Key key;
        private final long token;
        private final RegistrationKind kind;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private Registration(FandStructureService owner, Key key, long token, RegistrationKind kind) {
            this.owner = owner;
            this.key = key;
            this.token = token;
            this.kind = kind;
        }

        @Override
        public Key key() {
            return key;
        }

        @Override
        public boolean active() {
            return active.get() && owner.active(key, token, kind);
        }

        @Override
        public void unregister() {
            if (active.compareAndSet(true, false)) {
                if (owner.unregister(key, token, kind)) {
                    var current = owner.server.get();
                    if (current != null) {
                        callOnServerThread(current, () -> {
                            refreshStructureStates(current);
                            return null;
                        });
                    }
                }
            }
        }
    }
}
