package io.fand.server.structure;

import com.mojang.datafixers.util.Pair;
import io.fand.api.structure.StructurePlacement;
import io.fand.api.structure.StructureService;
import io.fand.api.structure.StructureTemplate;
import io.fand.api.structure.StructureVolume;
import io.fand.api.world.Location;
import io.fand.server.world.FandWorld;
import java.util.List;
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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

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
                .map(template -> {
                    var size = template.getSize();
                    return new StructureTemplate(key, size.getX(), size.getY(), size.getZ());
                }));
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

    private boolean placeOnServerThread(Key key, Location origin, StructurePlacement placement) {
        var level = level(origin.world());
        var template = level.getStructureManager().get(identifier(key)).orElse(null);
        if (template == null) {
            return false;
        }
        var pos = new BlockPos(origin.blockX(), origin.blockY(), origin.blockZ());
        var settings = new StructurePlaceSettings()
                .setRotation(rotation(placement.rotationDegrees()))
                .setMirror(placement.mirror() ? Mirror.FRONT_BACK : Mirror.NONE)
                .setIgnoreEntities(!placement.includeEntities())
                .setKnownShape(true);
        return template.placeInWorld(
                level,
                pos,
                pos,
                settings,
                net.minecraft.world.level.block.entity.StructureBlockEntity.createRandom(0),
                Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
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

    private static Rotation rotation(float degrees) {
        var normalized = ((degrees % 360.0F) + 360.0F) % 360.0F;
        if (normalized >= 45.0F && normalized < 135.0F) {
            return Rotation.CLOCKWISE_90;
        }
        if (normalized >= 135.0F && normalized < 225.0F) {
            return Rotation.CLOCKWISE_180;
        }
        if (normalized >= 225.0F && normalized < 315.0F) {
            return Rotation.COUNTERCLOCKWISE_90;
        }
        return Rotation.NONE;
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
