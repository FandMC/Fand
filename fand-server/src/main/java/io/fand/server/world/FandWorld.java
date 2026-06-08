package io.fand.server.world;

import io.fand.api.block.Block;
import io.fand.api.entity.Entity;
import io.fand.api.entity.ItemEntity;
import io.fand.api.entity.EntitySpawnOptions;
import io.fand.api.entity.EntityType;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Difficulty;
import io.fand.api.world.Location;
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
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
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
        if (radius < 0.0) {
            throw new IllegalArgumentException("radius must be >= 0, got " + radius);
        }
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
        if (radius < 0.0) {
            throw new IllegalArgumentException("radius must be >= 0, got " + radius);
        }
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
        var future = new CompletableFuture<Void>();
        runOnServerThread(() -> {
            try {
                task.run();
                future.complete(null);
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
}
