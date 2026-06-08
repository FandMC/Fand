package io.fand.server.world;

import io.fand.api.block.Block;
import io.fand.api.entity.Entity;
import io.fand.api.entity.ItemEntity;
import io.fand.api.entity.EntitySpawnOptions;
import io.fand.api.entity.EntityType;
import io.fand.api.entity.Player;
import io.fand.api.event.block.BlockFace;
import io.fand.api.item.ItemStack;
import io.fand.api.world.BlockRayTraceResult;
import io.fand.api.world.Difficulty;
import io.fand.api.world.EntityRayTraceResult;
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
import io.fand.server.entity.EntitySpawnOptionsApplier;
import io.fand.server.entity.FandEntityType;
import io.fand.server.entity.PlayerRegistry;
import io.fand.server.item.FandItemStacks;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jspecify.annotations.Nullable;

public final class FandWorld implements World {

    private final ServerLevel handle;
    private final Key key;
    private final @Nullable PlayerRegistry players;
    private final @Nullable WorldRegistry worldRegistry;
    private final WorldBorder worldBorder;

    public FandWorld(ServerLevel handle) {
        this(handle, null, null);
    }

    public FandWorld(ServerLevel handle, @Nullable PlayerRegistry players) {
        this(handle, players, null);
    }

    public FandWorld(ServerLevel handle, @Nullable PlayerRegistry players, @Nullable WorldRegistry worldRegistry) {
        this.handle = handle;
        this.players = players;
        this.worldRegistry = worldRegistry;
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
        return handle.getGameTime();
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
        var clock = handle.dimensionType().defaultClock()
                .orElseGet(() -> handle.registryAccess().getOrThrow(WorldClocks.OVERWORLD));
        return handle.getServer().clockManager().getTotalTicks(clock);
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
        return Difficulties.toApi(handle.getDifficulty());
    }

    @Override
    public CompletableFuture<Void> setDifficulty(Difficulty difficulty) {
        Objects.requireNonNull(difficulty, "difficulty");
        return runOnServerThreadFuture(() -> handle.getServer().setDifficulty(Difficulties.toVanilla(difficulty), false));
    }

    @Override
    public boolean storm() {
        return handle.isRaining();
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
        return handle.isThundering();
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
        if (players == null) {
            return List.of();
        }
        return players.snapshot(handle);
    }

    @Override
    public Collection<? extends Entity> entities() {
        return streamEntities(handle.getAllEntities());
    }

    @Override
    public Collection<? extends Entity> entities(EntityType type) {
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandEntityType fandType)) {
            return List.of();
        }
        return streamEntities(handle.getAllEntities(), entity -> entity.getType() == fandType.handle());
    }

    @Override
    public Optional<? extends Entity> entity(java.util.UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return Optional.ofNullable(handle.getEntity(uniqueId)).map(this::wrapEntity);
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
        return streamEntities(handle.getEntities((net.minecraft.world.entity.Entity) null, box, entity -> true));
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
        return streamEntities(handle.getEntities(
                (net.minecraft.world.entity.Entity) null,
                box,
                entity -> entity.getType() == fandType.handle()));
    }

    @Override
    public Collection<? extends Entity> entitiesInBox(Location min, Location max) {
        return streamEntities(handle.getEntities(
                (net.minecraft.world.entity.Entity) null,
                box(min, max),
                entity -> true));
    }

    @Override
    public Collection<? extends Entity> entitiesInBox(Location min, Location max, EntityType type) {
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandEntityType fandType)) {
            return List.of();
        }
        return streamEntities(handle.getEntities(
                (net.minecraft.world.entity.Entity) null,
                box(min, max),
                entity -> entity.getType() == fandType.handle()));
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
    }

    @Override
    public Optional<EntityRayTraceResult> rayTraceEntity(Location start, Vector3 direction, double maxDistance) {
        return rayTraceEntity(start, direction, maxDistance, entity -> true);
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
            var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(handle, EntitySpawnReason.EVENT);
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
        return handle.getChunkSource().hasChunk(chunkX, chunkZ);
    }

    @Override
    public int loadedEntityCount() {
        int count = 0;
        for (var ignored : handle.getAllEntities()) {
            count++;
        }
        return count;
    }

    @Override
    public int entityCount(int chunkX, int chunkZ) {
        if (!chunkLoaded(chunkX, chunkZ)) {
            return 0;
        }
        return handle.getEntities((net.minecraft.world.entity.Entity) null, chunkBox(chunkX, chunkZ), entity -> true).size();
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

    private Location requireThisWorld(Location location) {
        Objects.requireNonNull(location, "location");
        if (!location.world().key().equals(key)) {
            throw new IllegalArgumentException("Location world " + location.world().key().asString()
                    + " does not match " + key.asString());
        }
        requireFinite(location, "location");
        return location;
    }

    private void runOnServerThread(Runnable task) {
        var server = handle.getServer();
        if (server == null || server.isSameThread()) {
            task.run();
        } else {
            server.executeIfPossible(task);
        }
    }

    private CompletableFuture<Void> runOnServerThreadFuture(Runnable task) {
        var server = handle.getServer();
        if (server == null || server.isSameThread()) {
            try {
                task.run();
                return CompletableFuture.completedFuture(null);
            } catch (Throwable failure) {
                return CompletableFuture.failedFuture(failure);
            }
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        server.executeIfPossible(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    private <T> CompletableFuture<T> runOnServerThreadFuture(Supplier<T> task) {
        var server = handle.getServer();
        if (server == null || server.isSameThread()) {
            try {
                return CompletableFuture.completedFuture(task.get());
            } catch (Throwable failure) {
                return CompletableFuture.failedFuture(failure);
            }
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        server.executeIfPossible(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    private Entity wrapEntity(net.minecraft.world.entity.Entity entity) {
        if (worldRegistry != null) {
            return worldRegistry.entityRegistry().wrap(entity);
        }
        var fallbackRegistry = new WorldRegistry(
                handle.getServer(),
                players != null ? players : new PlayerRegistry(new io.fand.server.permission.PermissionManager())
        );
        return fallbackRegistry.entityRegistry().wrap(entity);
    }

    private Collection<? extends Entity> streamEntities(Iterable<net.minecraft.world.entity.Entity> entities) {
        return streamEntities(entities, entity -> true);
    }

    private Collection<? extends Entity> streamEntities(
            Iterable<net.minecraft.world.entity.Entity> entities,
            java.util.function.Predicate<net.minecraft.world.entity.Entity> filter
    ) {
        var snapshot = new java.util.ArrayList<Entity>();
        for (var entity : entities) {
            if (filter.test(entity)) {
                snapshot.add(wrapEntity(entity));
            }
        }
        return List.copyOf(snapshot);
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
        for (var entity : handle.getEntities((net.minecraft.world.entity.Entity) null, box, filter)) {
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
        var from = toVec3(checkedStart);
        var to = from.add(normalizedDirection.scale(checkedDistance));
        var searchBox = new AABB(from, to).inflate(1.0);
        double nearestDistanceSquared = checkedDistance * checkedDistance;
        net.minecraft.world.entity.Entity nearest = null;
        Vec3 nearestHit = null;
        for (var entity : handle.getEntities(
                (net.minecraft.world.entity.Entity) null,
                searchBox,
                entity -> !entity.isRemoved() && filter.test(entity))) {
            var hit = entity.getBoundingBox().inflate(Math.max(0.0F, entity.getPickRadius())).clip(from, to);
            if (hit.isPresent()) {
                double distanceSquared = from.distanceToSqr(hit.get());
                if (distanceSquared <= nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearest = entity;
                    nearestHit = hit.get();
                }
            }
        }
        if (nearest == null || nearestHit == null) {
            return Optional.empty();
        }
        return Optional.of(new EntityRayTraceResult(
                wrapEntity(nearest),
                toLocation(nearestHit),
                Math.sqrt(nearestDistanceSquared)));
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
