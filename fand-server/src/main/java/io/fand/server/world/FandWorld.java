package io.fand.server.world;

import io.fand.api.block.Block;
import io.fand.api.component.DataComponentKey;
import io.fand.api.component.DataComponentMap;
import io.fand.api.entity.Entity;
import io.fand.api.entity.ItemEntity;
import io.fand.api.entity.EntitySpawnOptions;
import io.fand.api.entity.EntityType;
import io.fand.api.entity.Player;
import io.fand.api.event.block.BlockFace;
import io.fand.api.item.ItemStack;
import io.fand.api.world.BlockBatchChange;
import io.fand.api.world.BlockBatchOptions;
import io.fand.api.world.BlockBatchResult;
import io.fand.api.world.BlockClipboard;
import io.fand.api.world.BlockRayTraceResult;
import io.fand.api.world.ChunkSnapshot;
import io.fand.api.world.Difficulty;
import io.fand.api.world.EntityRayTraceResult;
import io.fand.api.world.HeightmapType;
import io.fand.api.world.Location;
import io.fand.api.world.RayTraceBlockMode;
import io.fand.api.world.RayTraceFluidMode;
import io.fand.api.world.Vector3;
import io.fand.api.world.World;
import io.fand.api.world.WorldBorder;
import io.fand.api.world.particle.ParticleEffect;
import io.fand.api.world.particle.ParticleEmission;
import io.fand.api.world.sound.SoundEffect;
import io.fand.server.block.FandBlock;
import io.fand.server.block.FandBlockType;
import io.fand.server.component.BlockComponentStorage;
import io.fand.server.entity.EntitySpawnOptionsApplier;
import io.fand.server.entity.FandEntityType;
import io.fand.server.entity.PlayerRegistry;
import io.fand.server.item.FandItemStacks;
import io.fand.server.scheduler.TaskScheduler;
import io.fand.server.util.ServerThreading;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jspecify.annotations.Nullable;

public final class FandWorld implements World {

    private static final double ENTITY_RAY_SEGMENT_BLOCKS = 16.0;
    private static final EntityTypeTest<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> ALL_ENTITY_TYPE =
            EntityTypeTest.forClass(net.minecraft.world.entity.Entity.class);

    private final ServerLevel handle;
    private final Key key;
    private final @Nullable PlayerRegistry players;
    private final @Nullable WorldRegistry worldRegistry;
    private final @Nullable TaskScheduler scheduler;
    private final WorldBorder worldBorder;

    public FandWorld(ServerLevel handle) {
        this(handle, null, null, null);
    }

    public FandWorld(ServerLevel handle, @Nullable PlayerRegistry players) {
        this(handle, players, null, null);
    }

    public FandWorld(ServerLevel handle, @Nullable PlayerRegistry players, @Nullable WorldRegistry worldRegistry) {
        this(handle, players, worldRegistry, null);
    }

    public FandWorld(
            ServerLevel handle,
            @Nullable PlayerRegistry players,
            @Nullable WorldRegistry worldRegistry,
            @Nullable TaskScheduler scheduler
    ) {
        this.handle = handle;
        this.players = players;
        this.worldRegistry = worldRegistry;
        this.scheduler = scheduler;
        this.worldBorder = new FandWorldBorder(handle);
        var identifier = handle.dimension().identifier();
        this.key = Key.key(identifier.getNamespace(), identifier.getPath());
    }

    public ServerLevel handle() {
        return handle;
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public long seed() {
        return handle.getSeed();
    }

    @Override
    public long gameTime() {
        return callOnServerThread(handle::getGameTime);
    }

    @Override
    public CompletableFuture<Void> setGameTime(long ticks) {
        return runOnServerThreadFuture(() -> {
            if (handle.getLevelData() instanceof ServerLevelData data) {
                data.setGameTime(ticks);
            }
        });
    }

    @Override
    public long time() {
        return callOnServerThread(() -> {
            var clock = handle.dimensionType().defaultClock()
                    .orElseGet(() -> handle.registryAccess().getOrThrow(WorldClocks.OVERWORLD));
            return handle.getServer().clockManager().getTotalTicks(clock);
        });
    }

    @Override
    public CompletableFuture<Void> setTime(long ticks) {
        return runOnServerThreadFuture(() -> {
            var clock = handle.dimensionType().defaultClock()
                    .orElseGet(() -> handle.registryAccess().getOrThrow(WorldClocks.OVERWORLD));
            handle.getServer().clockManager().setTotalTicks(clock, ticks);
        });
    }

    @Override
    public Difficulty difficulty() {
        return callOnServerThread(() -> Difficulties.toApi(handle.getDifficulty()));
    }

    @Override
    public CompletableFuture<Void> setDifficulty(Difficulty difficulty) {
        Objects.requireNonNull(difficulty, "difficulty");
        return runOnServerThreadFuture(() -> handle.getServer().setDifficulty(Difficulties.toVanilla(difficulty), false));
    }

    @Override
    public boolean storm() {
        return callOnServerThread(handle::isRaining);
    }

    @Override
    public CompletableFuture<Void> setStorm(boolean storm) {
        return runOnServerThreadFuture(() -> {
            var server = handle.getServer();
            server.setWeatherParameters(storm ? 0 : 6000, storm ? 12000 : 0, storm, storm && handle.isThundering());
        });
    }

    @Override
    public boolean thundering() {
        return callOnServerThread(handle::isThundering);
    }

    @Override
    public CompletableFuture<Void> setThundering(boolean thundering) {
        return runOnServerThreadFuture(() -> {
            var server = handle.getServer();
            boolean raining = thundering || handle.isRaining();
            server.setWeatherParameters(0, raining ? 12000 : 0, raining, thundering);
        });
    }

    @Override
    public WorldBorder worldBorder() {
        return worldBorder;
    }

    @Override
    public Key biomeAt(int x, int y, int z) {
        return callOnServerThread(() -> {
            var biome = handle.getBiome(new BlockPos(x, y, z));
            var id = biome.unwrapKey()
                    .map(key -> key.identifier())
                    .orElseGet(() -> Identifier.withDefaultNamespace("plains"));
            return Key.key(id.getNamespace(), id.getPath());
        });
    }

    @Override
    public int highestBlockYAt(int x, int z, HeightmapType type) {
        Objects.requireNonNull(type, "type");
        return callOnServerThread(() -> handle.getHeight(heightmap(type), x, z));
    }

    @Override
    public Location spawnLocation() {
        return callOnServerThread(() -> {
            var respawn = handle.getServer().getRespawnData();
            var level = handle.getServer().getLevel(respawn.dimension());
            var world = level == null ? this : wrapWorld(level);
            var pos = respawn.pos();
            return world.at(pos.getX(), pos.getY(), pos.getZ(), respawn.yaw(), respawn.pitch());
        });
    }

    @Override
    public CompletableFuture<Void> setSpawnLocation(Location location) {
        var checkedLocation = requireFiniteLocation(location);
        return runOnServerThreadFuture(() -> {
            if (!(checkedLocation.world() instanceof FandWorld fandWorld)) {
                throw new IllegalArgumentException("Spawn location world must be a Fand world");
            }
            var pos = BlockPos.containing(checkedLocation.x(), checkedLocation.y(), checkedLocation.z());
            handle.getServer().setRespawnData(LevelData.RespawnData.of(
                    fandWorld.handle().dimension(),
                    pos,
                    checkedLocation.yaw(),
                    checkedLocation.pitch()));
        });
    }

    @Override
    public Optional<String> gameRule(String name) {
        Objects.requireNonNull(name, "name");
        return callOnServerThread(() -> gameRuleByName(name).map(rule -> handle.getGameRules().getAsString(rule)));
    }

    @Override
    public CompletableFuture<Boolean> setGameRule(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        return runOnServerThreadFuture(() -> gameRuleByName(name)
                .map(rule -> setGameRuleValue(rule, value))
                .orElse(false));
    }

    @Override
    public CompletableFuture<Boolean> save() {
        var future = new CompletableFuture<Boolean>();
        runOnServerThread(() -> {
            try {
                future.complete(handle.getServer().fand$saveLevelToDisk(handle.dimension()));
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    @Override
    public Collection<? extends Player> players() {
        return callOnServerThread(() -> players == null ? List.of() : players.snapshot(handle));
    }

    @Override
    public Collection<? extends Entity> entities() {
        return callOnServerThread(() -> streamEntities(handle.getAllEntities()));
    }

    @Override
    public Collection<? extends Entity> entities(EntityType type) {
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandEntityType fandType)) {
            return List.of();
        }
        return callOnServerThread(() -> streamEntities(handle.getAllEntities(), entity -> entity.getType() == fandType.handle()));
    }

    @Override
    public Optional<? extends Entity> entity(java.util.UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return callOnServerThread(() -> Optional.ofNullable(handle.getEntity(uniqueId)).map(this::wrapEntity));
    }

    @Override
    public Collection<? extends Entity> nearbyEntities(Location center, double radius) {
        var checkedCenter = requireThisWorld(center);
        requireNonNegativeFinite(radius, "radius");
        var box = new AABB(
                checkedCenter.x() - radius,
                checkedCenter.y() - radius,
                checkedCenter.z() - radius,
                checkedCenter.x() + radius,
                checkedCenter.y() + radius,
                checkedCenter.z() + radius
        );
        return callOnServerThread(() -> entitiesInBox(box, entity -> true));
    }

    @Override
    public Collection<? extends Entity> nearbyEntities(Location center, double radius, EntityType type) {
        var checkedCenter = requireThisWorld(center);
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandEntityType fandType)) {
            return List.of();
        }
        requireNonNegativeFinite(radius, "radius");
        var box = new AABB(
                checkedCenter.x() - radius,
                checkedCenter.y() - radius,
                checkedCenter.z() - radius,
                checkedCenter.x() + radius,
                checkedCenter.y() + radius,
                checkedCenter.z() + radius
        );
        return callOnServerThread(() -> entitiesInBox(box, entity -> entity.getType() == fandType.handle()));
    }

    @Override
    public Collection<? extends Entity> entitiesInBox(Location min, Location max) {
        return callOnServerThread(() -> entitiesInBox(box(min, max), entity -> true));
    }

    @Override
    public Collection<? extends Entity> entitiesInBox(Location min, Location max, EntityType type) {
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandEntityType fandType)) {
            return List.of();
        }
        return callOnServerThread(() -> entitiesInBox(box(min, max), entity -> entity.getType() == fandType.handle()));
    }

    @Override
    public int countEntitiesInBox(Location min, Location max) {
        return callOnServerThread(() -> countEntitiesInBox(box(min, max), entity -> true));
    }

    @Override
    public int countEntitiesInBox(Location min, Location max, EntityType type) {
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandEntityType fandType)) {
            return 0;
        }
        return callOnServerThread(() -> countEntitiesInBox(box(min, max), entity -> entity.getType() == fandType.handle()));
    }

    @Override
    public Optional<? extends Entity> firstEntityInBox(Location min, Location max) {
        return callOnServerThread(() -> firstEntityInBox(box(min, max), entity -> true));
    }

    @Override
    public Optional<? extends Entity> firstEntityInBox(Location min, Location max, EntityType type) {
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandEntityType fandType)) {
            return Optional.empty();
        }
        return callOnServerThread(() -> firstEntityInBox(box(min, max), entity -> entity.getType() == fandType.handle()));
    }

    @Override
    public void forEachEntityInBox(Location min, Location max, Consumer<? super Entity> action) {
        Objects.requireNonNull(action, "action");
        callOnServerThread(() -> {
            forEachEntityInBox(box(min, max), entity -> true, action);
            return null;
        });
    }

    @Override
    public void forEachEntityInBox(Location min, Location max, EntityType type, Consumer<? super Entity> action) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(action, "action");
        if (!(type instanceof FandEntityType fandType)) {
            return;
        }
        callOnServerThread(() -> {
            forEachEntityInBox(box(min, max), entity -> entity.getType() == fandType.handle(), action);
            return null;
        });
    }

    @Override
    public Optional<? extends Entity> nearestEntity(Location center, double radius) {
        return nearestEntity(center, radius, entity -> true);
    }

    @Override
    public Optional<? extends Entity> nearestEntity(Location center, double radius, EntityType type) {
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandEntityType fandType)) {
            return Optional.empty();
        }
        return nearestEntity(center, radius, entity -> entity.getType() == fandType.handle());
    }

    @Override
    public Optional<BlockRayTraceResult> rayTraceBlock(
            Location start,
            Vector3 direction,
            double maxDistance,
            RayTraceBlockMode blockMode,
            RayTraceFluidMode fluidMode
    ) {
        var checkedStart = requireThisWorld(start);
        var normalizedDirection = requireDirection(direction);
        var checkedDistance = requireNonNegativeFinite(maxDistance, "maxDistance");
        Objects.requireNonNull(blockMode, "blockMode");
        Objects.requireNonNull(fluidMode, "fluidMode");
        if (checkedDistance == 0.0) {
            return Optional.empty();
        }
        var from = toVec3(checkedStart);
        var to = from.add(normalizedDirection.scale(checkedDistance));
        return callOnServerThread(() -> {
            var result = handle.clip(new ClipContext(
                    from,
                    to,
                    blockMode(blockMode),
                    fluidMode(fluidMode),
                    CollisionContext.empty()));
            if (result.getType() != HitResult.Type.BLOCK) {
                return Optional.empty();
            }
            var hitLocation = toLocation(result.getLocation());
            var blockPos = result.getBlockPos();
            return Optional.of(new BlockRayTraceResult(
                    blockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                    hitLocation,
                    face(result.getDirection()),
                    result.isInside()));
        });
    }

    @Override
    public Optional<EntityRayTraceResult> rayTraceEntity(Location start, Vector3 direction, double maxDistance) {
        return rayTraceEntity(start, direction, maxDistance, entity -> true);
    }

    @Override
    public CompletableFuture<Optional<EntityRayTraceResult>> rayTraceEntityAsync(
            Location start,
            Vector3 direction,
            double maxDistance
    ) {
        return rayTraceEntityAsync(start, direction, maxDistance, entity -> true);
    }

    @Override
    public Optional<EntityRayTraceResult> rayTraceEntity(
            Location start,
            Vector3 direction,
            double maxDistance,
            EntityType type
    ) {
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandEntityType fandType)) {
            return Optional.empty();
        }
        return rayTraceEntity(start, direction, maxDistance, entity -> entity.getType() == fandType.handle());
    }

    @Override
    public CompletableFuture<Optional<EntityRayTraceResult>> rayTraceEntityAsync(
            Location start,
            Vector3 direction,
            double maxDistance,
            EntityType type
    ) {
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandEntityType fandType)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return rayTraceEntityAsync(start, direction, maxDistance, entity -> entity.getType() == fandType.handle());
    }

    @Override
    public CompletableFuture<Optional<? extends Entity>> spawnEntity(Location location, EntityType type) {
        return spawnEntity(location, type, EntitySpawnOptions.defaults());
    }

    @Override
    public CompletableFuture<Optional<? extends Entity>> spawnEntity(
            Location location,
            EntityType type,
            EntitySpawnOptions options
    ) {
        var checkedLocation = requireThisWorld(location);
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(options, "options");
        if (!(type instanceof FandEntityType fandType) || !type.spawnable()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        var future = new CompletableFuture<Optional<? extends Entity>>();
        runOnServerThread(() -> {
            try {
                var entity = fandType.handle().create(handle, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (entity == null) {
                    future.complete(Optional.empty());
                    return;
                }
                entity.teleportTo(checkedLocation.x(), checkedLocation.y(), checkedLocation.z());
                entity.forceSetRotation(checkedLocation.yaw(), false, checkedLocation.pitch(), false);
                EntitySpawnOptionsApplier.apply(entity, options);
                if (!handle.addFreshEntity(entity)) {
                    future.complete(Optional.empty());
                    return;
                }
                var wrapped = wrapEntity(entity);
                future.complete(Optional.of(wrapped));
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Optional<? extends ItemEntity>> dropItem(Location location, ItemStack item) {
        return dropItem(location, item, EntitySpawnOptions.defaults());
    }

    @Override
    public CompletableFuture<Optional<? extends ItemEntity>> dropItem(
            Location location,
            ItemStack item,
            EntitySpawnOptions options
    ) {
        var checkedLocation = requireThisWorld(location);
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(options, "options");
        if (item.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        var future = new CompletableFuture<Optional<? extends ItemEntity>>();
        runOnServerThread(() -> {
            try {
                var vanilla = FandItemStacks.toVanilla(item);
                if (vanilla.isEmpty()) {
                    future.complete(Optional.empty());
                    return;
                }
                var entity = new net.minecraft.world.entity.item.ItemEntity(
                        handle,
                        checkedLocation.x(),
                        checkedLocation.y(),
                        checkedLocation.z(),
                        vanilla);
                entity.setDefaultPickUpDelay();
                EntitySpawnOptionsApplier.apply(entity, options);
                if (!handle.addFreshEntity(entity)) {
                    future.complete(Optional.empty());
                    return;
                }
                var wrapped = wrapEntity(entity);
                if (wrapped instanceof ItemEntity itemEntity) {
                    future.complete(Optional.of(itemEntity));
                } else {
                    future.completeExceptionally(new IllegalStateException("Dropped item wrapped as non-item entity: " + entity));
                }
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    @Override
    public Iterable<? extends Audience> audiences() {
        return players();
    }

    @Override
    public void playSound(Location location, SoundEffect sound) {
        Objects.requireNonNull(sound, "sound");
        var checkedLocation = requireThisWorld(location);
        runOnServerThread(() -> SoundEffects.play(handle, checkedLocation, sound));
    }

    @Override
    public void spawnParticle(Location location, ParticleEffect effect, ParticleEmission emission) {
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(emission, "emission");
        var checkedLocation = requireThisWorld(location);
        runOnServerThread(() -> ParticleEffects.spawn(handle, checkedLocation, effect, emission));
    }

    @Override
    public CompletableFuture<Optional<? extends Entity>> strikeLightning(Location location, boolean visualOnly) {
        var checkedLocation = requireThisWorld(location);
        return this.<Optional<? extends Entity>>runOnServerThreadFuture(() -> {
            var bolt = EntityTypes.LIGHTNING_BOLT.create(handle, EntitySpawnReason.EVENT);
            if (bolt == null) {
                return Optional.empty();
            }
            bolt.snapTo(toVec3(checkedLocation));
            bolt.setVisualOnly(visualOnly);
            if (!handle.addFreshEntity(bolt)) {
                return Optional.empty();
            }
            return Optional.of(wrapEntity(bolt));
        });
    }

    @Override
    public CompletableFuture<Void> createExplosion(Location location, float power, boolean fire, boolean breakBlocks) {
        var checkedLocation = requireThisWorld(location);
        if (!Float.isFinite(power) || power < 0.0F) {
            throw new IllegalArgumentException("power must be finite and >= 0");
        }
        return runOnServerThreadFuture(() -> handle.explode(
                null,
                checkedLocation.x(),
                checkedLocation.y(),
                checkedLocation.z(),
                power,
                fire,
                breakBlocks ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE));
    }

    @Override
    public boolean chunkLoaded(int chunkX, int chunkZ) {
        return callOnServerThread(() -> handle.getChunkSource().hasChunk(chunkX, chunkZ));
    }

    @Override
    public CompletableFuture<Boolean> loadChunk(int chunkX, int chunkZ) {
        return runOnServerThreadFuture(() -> {
            var chunk = handle.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            return chunk != null;
        });
    }

    @Override
    public CompletableFuture<Boolean> unloadChunk(int chunkX, int chunkZ) {
        return runOnServerThreadFuture(() -> {
            boolean updated = handle.setChunkForced(chunkX, chunkZ, false);
            handle.getChunkSource().tick(() -> true, false);
            return updated;
        });
    }

    @Override
    public boolean chunkForceLoaded(int chunkX, int chunkZ) {
        return callOnServerThread(() -> handle.getForceLoadedChunks().contains(ChunkPos.pack(chunkX, chunkZ)));
    }

    @Override
    public CompletableFuture<Boolean> setChunkForceLoaded(int chunkX, int chunkZ, boolean forceLoaded) {
        return runOnServerThreadFuture(() -> handle.setChunkForced(chunkX, chunkZ, forceLoaded));
    }

    @Override
    public int loadedEntityCount() {
        return callOnServerThread(() -> {
            int count = 0;
            for (var ignored : handle.getAllEntities()) {
                count++;
            }
            return count;
        });
    }

    @Override
    public int entityCount(int chunkX, int chunkZ) {
        if (!chunkLoaded(chunkX, chunkZ)) {
            return 0;
        }
        return callOnServerThread(() -> countEntitiesInBox(chunkBox(chunkX, chunkZ), entity -> true));
    }

    @Override
    public Collection<? extends Entity> entitiesInChunk(int chunkX, int chunkZ) {
        if (!chunkLoaded(chunkX, chunkZ)) {
            return List.of();
        }
        return callOnServerThread(() -> entitiesInBox(chunkBox(chunkX, chunkZ), entity -> true));
    }

    @Override
    public ChunkSnapshot chunkSnapshot(int chunkX, int chunkZ) {
        return callOnServerThread(() -> {
            boolean loaded = handle.getChunkSource().hasChunk(chunkX, chunkZ);
            boolean forceLoaded = handle.getForceLoadedChunks().contains(ChunkPos.pack(chunkX, chunkZ));
            int entities = loaded ? countEntitiesInBox(chunkBox(chunkX, chunkZ), entity -> true) : 0;
            return new ChunkSnapshot(this, chunkX, chunkZ, loaded, forceLoaded, entities);
        });
    }

    @Override
    public Collection<? extends Block> blocksWith(DataComponentKey<?> key) {
        Objects.requireNonNull(key, "key");
        return callOnServerThread(() -> BlockComponentStorage.positionsWith(handle, key).stream()
                .map(pos -> new FandBlock(this, pos.getX(), pos.getY(), pos.getZ()))
                .toList());
    }

    @Override
    public Collection<? extends Block> blocksWith(DataComponentKey<?> key, int chunkX, int chunkZ) {
        Objects.requireNonNull(key, "key");
        return callOnServerThread(() -> BlockComponentStorage.positionsWith(handle, key, new ChunkPos(chunkX, chunkZ)).stream()
                .map(pos -> new FandBlock(this, pos.getX(), pos.getY(), pos.getZ()))
                .toList());
    }

    @Override
    public CompletableFuture<BlockBatchResult> setBlocks(Collection<BlockBatchChange> changes) {
        return setBlocks(changes, BlockBatchOptions.defaults());
    }

    @Override
    public CompletableFuture<BlockBatchResult> setBlocks(
            Collection<BlockBatchChange> changes,
            BlockBatchOptions options
    ) {
        Objects.requireNonNull(changes, "changes");
        Objects.requireNonNull(options, "options");
        if (changes.isEmpty()) {
            return CompletableFuture.completedFuture(BlockBatchResult.empty());
        }
        var snapshot = List.copyOf(changes);
        return runBlockBatch(snapshot.size(), snapshot.iterator(), options);
    }

    @Override
    public CompletableFuture<BlockBatchResult> fillBlocks(Location min, Location max, io.fand.api.block.BlockType type) {
        return fillBlocks(min, max, type, DataComponentMap.EMPTY, BlockBatchOptions.defaults());
    }

    @Override
    public CompletableFuture<BlockBatchResult> fillBlocks(
            Location min,
            Location max,
            io.fand.api.block.BlockType type,
            DataComponentMap components,
            BlockBatchOptions options
    ) {
        var checkedMin = requireThisWorld(min);
        var checkedMax = requireThisWorld(max);
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(components, "components");
        Objects.requireNonNull(options, "options");
        int minX = Math.min(checkedMin.blockX(), checkedMax.blockX());
        int minY = Math.min(checkedMin.blockY(), checkedMax.blockY());
        int minZ = Math.min(checkedMin.blockZ(), checkedMax.blockZ());
        int maxX = Math.max(checkedMin.blockX(), checkedMax.blockX());
        int maxY = Math.max(checkedMin.blockY(), checkedMax.blockY());
        int maxZ = Math.max(checkedMin.blockZ(), checkedMax.blockZ());
        long requested = volume(minX, minY, minZ, maxX, maxY, maxZ);
        if (requested == 0L) {
            return CompletableFuture.completedFuture(BlockBatchResult.empty());
        }
        if (requested > Integer.MAX_VALUE) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("batch contains too many blocks: " + requested));
        }
        return runBlockBatch(
                (int) requested,
                new FillBlockIterator(minX, minY, minZ, maxX, maxY, maxZ, type, components),
                options);
    }

    @Override
    public CompletableFuture<BlockBatchResult> pasteBlocks(Location origin, BlockClipboard clipboard) {
        return pasteBlocks(origin, clipboard, BlockBatchOptions.defaults());
    }

    @Override
    public CompletableFuture<BlockBatchResult> pasteBlocks(
            Location origin,
            BlockClipboard clipboard,
            BlockBatchOptions options
    ) {
        var checkedOrigin = requireThisWorld(origin);
        Objects.requireNonNull(clipboard, "clipboard");
        Objects.requireNonNull(options, "options");
        if (clipboard.empty()) {
            return CompletableFuture.completedFuture(BlockBatchResult.empty());
        }
        return runBlockBatch(
                clipboard.blocks().size(),
                new OffsetBlockIterator(clipboard.blocks().iterator(), checkedOrigin.blockX(), checkedOrigin.blockY(), checkedOrigin.blockZ()),
                options);
    }

    @Override
    public Block blockAt(int x, int y, int z) {
        return new FandBlock(this, x, y, z);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandWorld that && this.key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "FandWorld(" + key.asString() + ")";
    }

    private CompletableFuture<BlockBatchResult> runBlockBatch(
            int requested,
            Iterator<BlockBatchChange> changes,
            BlockBatchOptions options
    ) {
        Objects.requireNonNull(changes, "changes");
        Objects.requireNonNull(options, "options");
        if (scheduler == null) {
            return runOnServerThreadFuture(() -> applyBlockBatchInline(requested, changes, options));
        }
        var future = new CompletableFuture<BlockBatchResult>();
        try {
            scheduler.runMain(new BlockBatchRunner(requested, changes, options, future));
        } catch (RejectedExecutionException failure) {
            future.completeExceptionally(failure);
        }
        return future;
    }

    private BlockBatchResult applyBlockBatchInline(
            int requested,
            Iterator<BlockBatchChange> changes,
            BlockBatchOptions options
    ) {
        int changed = 0;
        int skipped = 0;
        int failed = 0;
        while (changes.hasNext()) {
            try {
                if (applyBlockChange(changes.next(), options)) {
                    changed++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException failure) {
                failed++;
            }
        }
        return new BlockBatchResult(requested, changed, skipped, failed);
    }

    private boolean applyBlockChange(BlockBatchChange change, BlockBatchOptions options) {
        Objects.requireNonNull(change, "change");
        var block = FandBlockType.unwrap(change.type());
        var state = block.defaultBlockState();
        var pos = new BlockPos(change.x(), change.y(), change.z());
        if (options.skipUnchanged()) {
            var currentState = handle.getBlockState(pos);
            if (currentState.getBlock() == block && BlockComponentStorage.snapshot(handle, pos).equals(change.components())) {
                return false;
            }
        }
        if (!handle.setBlock(pos, state, blockUpdateFlags(options))) {
            throw new IllegalStateException("Failed to set block at " + pos.toShortString());
        }
        if (change.components().isEmpty()) {
            BlockComponentStorage.clear(handle, pos);
        } else {
            BlockComponentStorage.put(handle, pos, change.components());
        }
        return true;
    }

    private int blockUpdateFlags(BlockBatchOptions options) {
        return switch (options.updateMode()) {
            case NORMAL -> net.minecraft.world.level.block.Block.UPDATE_ALL;
            case CLIENTS_ONLY -> net.minecraft.world.level.block.Block.UPDATE_CLIENTS;
            case SILENT -> net.minecraft.world.level.block.Block.UPDATE_NONE;
        };
    }

    static long volume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        long volume = 1L;
        volume = multiplyVolumeCapped(volume, span(minX, maxX));
        volume = multiplyVolumeCapped(volume, span(minY, maxY));
        volume = multiplyVolumeCapped(volume, span(minZ, maxZ));
        return volume;
    }

    private static long multiplyVolumeCapped(long volume, long span) {
        if (volume > Integer.MAX_VALUE / span) {
            return (long) Integer.MAX_VALUE + 1L;
        }
        return volume * span;
    }

    private static long span(int min, int max) {
        return (long) max - min + 1L;
    }

    private Location requireThisWorld(Location location) {
        Objects.requireNonNull(location, "location");
        if (!location.world().key().equals(key)) {
            throw new IllegalArgumentException("Location world " + location.world().key().asString()
                    + " does not match " + key.asString());
        }
        requireFinite(location, "location");
        return location;
    }

    private Location requireFiniteLocation(Location location) {
        Objects.requireNonNull(location, "location");
        requireFinite(location, "location");
        return location;
    }

    private void runOnServerThread(Runnable task) {
        var server = handle.getServer();
        ServerThreading.run(server, task);
    }

    private CompletableFuture<Void> runOnServerThreadFuture(Runnable task) {
        var server = handle.getServer();
        return ServerThreading.runFuture(server, task);
    }

    private <T> CompletableFuture<T> runOnServerThreadFuture(Supplier<T> task) {
        var server = handle.getServer();
        return ServerThreading.callFuture(server, task);
    }

    private <T> T callOnServerThread(Supplier<T> task) {
        var server = handle.getServer();
        return ServerThreading.callBlocking(server, task);
    }

    private Entity wrapEntity(net.minecraft.world.entity.Entity entity) {
        if (worldRegistry != null) {
            return worldRegistry.entityRegistry().wrap(entity);
        }
        var fallbackRegistry = new WorldRegistry(
                handle.getServer(),
                players != null ? players : new PlayerRegistry(new io.fand.server.permission.PermissionManager()),
                scheduler
        );
        return fallbackRegistry.entityRegistry().wrap(entity);
    }

    private FandWorld wrapWorld(ServerLevel level) {
        if (level == handle) {
            return this;
        }
        if (worldRegistry != null) {
            return worldRegistry.wrap(level);
        }
        return new FandWorld(level, players, null, scheduler);
    }

    private Collection<? extends Entity> streamEntities(Iterable<net.minecraft.world.entity.Entity> entities) {
        return streamEntities(entities, entity -> true);
    }

    private Collection<? extends Entity> streamEntities(
            Iterable<net.minecraft.world.entity.Entity> entities,
            java.util.function.Predicate<net.minecraft.world.entity.Entity> filter
    ) {
        var snapshot = new java.util.ArrayList<Entity>(entities instanceof Collection<?> collection ? collection.size() : 16);
        for (var entity : entities) {
            if (filter.test(entity)) {
                snapshot.add(wrapEntity(entity));
            }
        }
        return java.util.Collections.unmodifiableList(snapshot);
    }

    private Collection<? extends Entity> entitiesInBox(
            AABB box,
            Predicate<net.minecraft.world.entity.Entity> filter
    ) {
        var candidates = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        collectEntitiesInBox(box, filter, candidates);
        return streamEntities(candidates);
    }

    private int countEntitiesInBox(
            AABB box,
            Predicate<net.minecraft.world.entity.Entity> filter
    ) {
        var candidates = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        collectEntitiesInBox(box, filter, candidates);
        return candidates.size();
    }

    private Optional<? extends Entity> firstEntityInBox(
            AABB box,
            Predicate<net.minecraft.world.entity.Entity> filter
    ) {
        var candidates = new java.util.ArrayList<net.minecraft.world.entity.Entity>(1);
        handle.getEntities(
                ALL_ENTITY_TYPE,
                box,
                entity -> !entity.isRemoved() && filter.test(entity),
                candidates,
                1);
        return candidates.isEmpty() ? Optional.empty() : Optional.of(wrapEntity(candidates.getFirst()));
    }

    private void forEachEntityInBox(
            AABB box,
            Predicate<net.minecraft.world.entity.Entity> filter,
            Consumer<? super Entity> action
    ) {
        var candidates = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        collectEntitiesInBox(box, filter, candidates);
        for (var entity : candidates) {
            action.accept(wrapEntity(entity));
        }
    }

    private void collectEntitiesInBox(
            AABB box,
            Predicate<net.minecraft.world.entity.Entity> filter,
            java.util.List<net.minecraft.world.entity.Entity> output
    ) {
        handle.getEntities(
                ALL_ENTITY_TYPE,
                box,
                entity -> !entity.isRemoved() && filter.test(entity),
                output);
    }

    private AABB box(Location min, Location max) {
        var checkedMin = requireThisWorld(min);
        var checkedMax = requireThisWorld(max);
        return new AABB(
                Math.min(checkedMin.x(), checkedMax.x()),
                Math.min(checkedMin.y(), checkedMax.y()),
                Math.min(checkedMin.z(), checkedMax.z()),
                Math.max(checkedMin.x(), checkedMax.x()),
                Math.max(checkedMin.y(), checkedMax.y()),
                Math.max(checkedMin.z(), checkedMax.z()));
    }

    private AABB chunkBox(int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        return new AABB(minX, handle.getMinY(), minZ, minX + 16, handle.getMaxY() + 1.0, minZ + 16);
    }

    private Optional<? extends Entity> nearestEntity(
            Location center,
            double radius,
            Predicate<net.minecraft.world.entity.Entity> filter
    ) {
        var checkedCenter = requireThisWorld(center);
        var checkedRadius = requireNonNegativeFinite(radius, "radius");
        var box = new AABB(
                checkedCenter.x() - checkedRadius,
                checkedCenter.y() - checkedRadius,
                checkedCenter.z() - checkedRadius,
                checkedCenter.x() + checkedRadius,
                checkedCenter.y() + checkedRadius,
                checkedCenter.z() + checkedRadius);
        double radiusSquared = checkedRadius * checkedRadius;
        double nearestDistanceSquared = Double.MAX_VALUE;
        net.minecraft.world.entity.Entity nearest = null;
        var candidates = new java.util.ArrayList<net.minecraft.world.entity.Entity>(32);
        collectEntitiesInBox(box, filter, candidates);
        for (var entity : candidates) {
            double distanceSquared = distanceSquared(checkedCenter, entity);
            if (distanceSquared <= radiusSquared && distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = entity;
            }
        }
        return Optional.ofNullable(nearest).map(this::wrapEntity);
    }

    private Optional<EntityRayTraceResult> rayTraceEntity(
            Location start,
            Vector3 direction,
            double maxDistance,
            Predicate<net.minecraft.world.entity.Entity> filter
    ) {
        var checkedStart = requireThisWorld(start);
        var normalizedDirection = requireDirection(direction);
        var checkedDistance = requireNonNegativeFinite(maxDistance, "maxDistance");
        if (checkedDistance == 0.0) {
            return Optional.empty();
        }
        return rayTraceEntityChecked(checkedStart, normalizedDirection, checkedDistance, filter);
    }

    private Optional<EntityRayTraceResult> rayTraceEntityChecked(
            Location checkedStart,
            Vec3 normalizedDirection,
            double checkedDistance,
            Predicate<net.minecraft.world.entity.Entity> filter
    ) {
        var from = toVec3(checkedStart);
        var to = from.add(normalizedDirection.scale(checkedDistance));
        double nearestDistanceSquared = checkedDistance * checkedDistance;
        net.minecraft.world.entity.Entity nearest = null;
        RayHit nearestHit = null;
        var candidates = new java.util.ArrayList<net.minecraft.world.entity.Entity>(32);
        for (double segmentStart = 0.0; segmentStart < checkedDistance; segmentStart += ENTITY_RAY_SEGMENT_BLOCKS) {
            double segmentEnd = Math.min(checkedDistance, segmentStart + ENTITY_RAY_SEGMENT_BLOCKS);
            var segmentFrom = from.add(normalizedDirection.scale(segmentStart));
            var segmentTo = from.add(normalizedDirection.scale(segmentEnd));
            var searchBox = new AABB(segmentFrom, segmentTo).inflate(1.0);
            candidates.clear();
            collectRaySegmentCandidates(searchBox, filter, candidates);
            for (var entity : candidates) {
                var hit = traceEntity(entity, from, to, nearestDistanceSquared);
                if (hit != null && hit.distanceSquared() <= nearestDistanceSquared) {
                    nearestDistanceSquared = hit.distanceSquared();
                    nearest = entity;
                    nearestHit = hit;
                }
            }
            if (nearestHit != null && nearestDistanceSquared <= segmentEnd * segmentEnd) {
                break;
            }
        }
        if (nearest == null || nearestHit == null) {
            return Optional.empty();
        }
        return Optional.of(new EntityRayTraceResult(
                wrapEntity(nearest),
                at(nearestHit.x(), nearestHit.y(), nearestHit.z()),
                Math.sqrt(nearestDistanceSquared)));
    }

    private CompletableFuture<Optional<EntityRayTraceResult>> rayTraceEntityAsync(
            Location start,
            Vector3 direction,
            double maxDistance,
            Predicate<net.minecraft.world.entity.Entity> filter
    ) {
        var checkedStart = requireThisWorld(start);
        var normalizedDirection = requireDirection(direction);
        var checkedDistance = requireNonNegativeFinite(maxDistance, "maxDistance");
        if (checkedDistance == 0.0) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return runOnServerThreadFuture(() -> rayTraceEntityChecked(checkedStart, normalizedDirection, checkedDistance, filter));
    }

    private void collectRaySegmentCandidates(
            AABB searchBox,
            Predicate<net.minecraft.world.entity.Entity> filter,
            java.util.List<net.minecraft.world.entity.Entity> output
    ) {
        collectEntitiesInBox(searchBox, filter, output);
    }

    private static @Nullable RayHit traceEntity(
            net.minecraft.world.entity.Entity entity,
            Vec3 from,
            Vec3 to,
            double maxDistanceSquared
    ) {
        var box = entity.getBoundingBox();
        double inflate = Math.max(0.0F, entity.getPickRadius());
        return traceBox(
                box.minX - inflate,
                box.minY - inflate,
                box.minZ - inflate,
                box.maxX + inflate,
                box.maxY + inflate,
                box.maxZ + inflate,
                from,
                to,
                maxDistanceSquared);
    }

    private static @Nullable RayHit traceBox(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            Vec3 from,
            Vec3 to,
            double maxDistanceSquared
    ) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double tMin = 0.0;
        double tMax = 1.0;

        if (Math.abs(dx) < 1.0E-12) {
            if (from.x < minX || from.x > maxX) {
                return null;
            }
        } else {
            double inv = 1.0 / dx;
            double near = (minX - from.x) * inv;
            double far = (maxX - from.x) * inv;
            if (near > far) {
                double swap = near;
                near = far;
                far = swap;
            }
            tMin = Math.max(tMin, near);
            tMax = Math.min(tMax, far);
            if (tMin > tMax) {
                return null;
            }
        }

        if (Math.abs(dy) < 1.0E-12) {
            if (from.y < minY || from.y > maxY) {
                return null;
            }
        } else {
            double inv = 1.0 / dy;
            double near = (minY - from.y) * inv;
            double far = (maxY - from.y) * inv;
            if (near > far) {
                double swap = near;
                near = far;
                far = swap;
            }
            tMin = Math.max(tMin, near);
            tMax = Math.min(tMax, far);
            if (tMin > tMax) {
                return null;
            }
        }

        if (Math.abs(dz) < 1.0E-12) {
            if (from.z < minZ || from.z > maxZ) {
                return null;
            }
        } else {
            double inv = 1.0 / dz;
            double near = (minZ - from.z) * inv;
            double far = (maxZ - from.z) * inv;
            if (near > far) {
                double swap = near;
                near = far;
                far = swap;
            }
            tMin = Math.max(tMin, near);
            tMax = Math.min(tMax, far);
            if (tMin > tMax) {
                return null;
            }
        }

        double distanceSquared = (dx * dx + dy * dy + dz * dz) * tMin * tMin;
        if (distanceSquared > maxDistanceSquared) {
            return null;
        }
        return new RayHit(
                from.x + dx * tMin,
                from.y + dy * tMin,
                from.z + dz * tMin,
                distanceSquared);
    }

    private Vec3 requireDirection(Vector3 direction) {
        Objects.requireNonNull(direction, "direction");
        if (!Double.isFinite(direction.x()) || !Double.isFinite(direction.y()) || !Double.isFinite(direction.z())) {
            throw new IllegalArgumentException("direction must be finite");
        }
        double length = direction.length();
        if (!Double.isFinite(length) || length == 0.0) {
            throw new IllegalArgumentException("direction must be non-zero");
        }
        return new Vec3(direction.x() / length, direction.y() / length, direction.z() / length);
    }

    private double requireNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and >= 0");
        }
        return value;
    }

    private void requireFinite(Location location, String name) {
        if (!Double.isFinite(location.x()) || !Double.isFinite(location.y()) || !Double.isFinite(location.z())) {
            throw new IllegalArgumentException(name + " coordinates must be finite");
        }
    }

    private double distanceSquared(Location center, net.minecraft.world.entity.Entity entity) {
        double dx = center.x() - entity.getX();
        double dy = center.y() - entity.getY();
        double dz = center.z() - entity.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private Vec3 toVec3(Location location) {
        return new Vec3(location.x(), location.y(), location.z());
    }

    private Location toLocation(Vec3 position) {
        return at(position.x, position.y, position.z);
    }

    private record RayHit(double x, double y, double z, double distanceSquared) {
    }

    private final class BlockBatchRunner implements Runnable {

        private final int requested;
        private final Iterator<BlockBatchChange> changes;
        private final BlockBatchOptions options;
        private final CompletableFuture<BlockBatchResult> future;
        private int changed;
        private int skipped;
        private int failed;

        private BlockBatchRunner(
                int requested,
                Iterator<BlockBatchChange> changes,
                BlockBatchOptions options,
                CompletableFuture<BlockBatchResult> future
        ) {
            this.requested = requested;
            this.changes = changes;
            this.options = options;
            this.future = future;
        }

        @Override
        public void run() {
            if (future.isDone()) {
                return;
            }
            try {
                applySlice();
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
                return;
            }
            if (!changes.hasNext()) {
                future.complete(new BlockBatchResult(requested, changed, skipped, failed));
                return;
            }
            scheduleNextSlice();
        }

        private void applySlice() {
            int remaining = options.maxBlocksPerTick();
            while (remaining > 0 && changes.hasNext()) {
                var change = changes.next();
                try {
                    if (applyBlockChange(change, options)) {
                        changed++;
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException failure) {
                    failed++;
                }
                remaining--;
            }
        }

        private void scheduleNextSlice() {
            if (scheduler != null) {
                try {
                    scheduler.runMainAfterTicks(this, 0L);
                } catch (RejectedExecutionException failure) {
                    future.completeExceptionally(failure);
                }
                return;
            }
            runOnServerThread(this);
        }
    }

    private static final class FillBlockIterator implements Iterator<BlockBatchChange> {

        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;
        private final io.fand.api.block.BlockType type;
        private final DataComponentMap components;
        private int x;
        private int y;
        private int z;
        private boolean hasNext = true;

        private FillBlockIterator(
                int minX,
                int minY,
                int minZ,
                int maxX,
                int maxY,
                int maxZ,
                io.fand.api.block.BlockType type,
                DataComponentMap components
        ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.type = type;
            this.components = components;
            this.x = minX;
            this.y = minY;
            this.z = minZ;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public BlockBatchChange next() {
            if (!hasNext) {
                throw new java.util.NoSuchElementException();
            }
            var change = BlockBatchChange.of(x, y, z, type, components);
            advance();
            return change;
        }

        private void advance() {
            if (x < maxX) {
                x++;
                return;
            }
            x = minX;
            if (z < maxZ) {
                z++;
                return;
            }
            z = minZ;
            if (y < maxY) {
                y++;
                return;
            }
            hasNext = false;
        }
    }

    private static final class OffsetBlockIterator implements Iterator<BlockBatchChange> {

        private final Iterator<BlockBatchChange> source;
        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;

        private OffsetBlockIterator(Iterator<BlockBatchChange> source, int offsetX, int offsetY, int offsetZ) {
            this.source = source;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        }

        @Override
        public boolean hasNext() {
            return source.hasNext();
        }

        @Override
        public BlockBatchChange next() {
            return source.next().offset(offsetX, offsetY, offsetZ);
        }
    }

    private ClipContext.Block blockMode(RayTraceBlockMode mode) {
        return switch (mode) {
            case COLLIDER -> ClipContext.Block.COLLIDER;
            case OUTLINE -> ClipContext.Block.OUTLINE;
            case VISUAL -> ClipContext.Block.VISUAL;
        };
    }

    private ClipContext.Fluid fluidMode(RayTraceFluidMode mode) {
        return switch (mode) {
            case NONE -> ClipContext.Fluid.NONE;
            case SOURCE_ONLY -> ClipContext.Fluid.SOURCE_ONLY;
            case ANY -> ClipContext.Fluid.ANY;
            case WATER -> ClipContext.Fluid.WATER;
        };
    }

    private Heightmap.Types heightmap(HeightmapType type) {
        return switch (type) {
            case WORLD_SURFACE_WG -> Heightmap.Types.WORLD_SURFACE_WG;
            case WORLD_SURFACE -> Heightmap.Types.WORLD_SURFACE;
            case OCEAN_FLOOR_WG -> Heightmap.Types.OCEAN_FLOOR_WG;
            case OCEAN_FLOOR -> Heightmap.Types.OCEAN_FLOOR;
            case MOTION_BLOCKING -> Heightmap.Types.MOTION_BLOCKING;
            case MOTION_BLOCKING_NO_LEAVES -> Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        };
    }

    private Optional<net.minecraft.world.level.gamerules.GameRule<?>> gameRuleByName(String name) {
        var exact = Identifier.tryParse(name);
        if (exact != null) {
            var found = BuiltInRegistries.GAME_RULE.getOptional(exact);
            if (found.isPresent()) {
                return found;
            }
        }
        var vanilla = Identifier.withDefaultNamespace(name);
        return BuiltInRegistries.GAME_RULE.getOptional(vanilla);
    }

    private <T> boolean setGameRuleValue(net.minecraft.world.level.gamerules.GameRule<T> rule, String value) {
        var parsed = rule.deserialize(value).result();
        if (parsed.isEmpty()) {
            return false;
        }
        handle.getGameRules().set(rule, parsed.get(), handle.getServer());
        return true;
    }

    private BlockFace face(Direction direction) {
        return switch (direction) {
            case DOWN -> BlockFace.DOWN;
            case UP -> BlockFace.UP;
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case EAST -> BlockFace.EAST;
        };
    }
}
