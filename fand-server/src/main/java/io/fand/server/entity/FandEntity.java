package io.fand.server.entity;

import io.fand.api.component.DataComponentContainer;
import io.fand.api.entity.EntityType;
import io.fand.api.world.Location;
import io.fand.api.world.Vector3;
import io.fand.api.world.World;
import io.fand.server.component.EntityComponentStorage;
import io.fand.server.world.FandWorld;
import io.fand.server.world.WorldRegistry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Thin handle around any vanilla entity for API consumers.
 */
public class FandEntity implements io.fand.api.entity.Entity {

    protected final net.minecraft.world.entity.Entity handle;
    protected final WorldRegistry worldRegistry;

    public FandEntity(net.minecraft.world.entity.Entity handle, WorldRegistry worldRegistry) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.worldRegistry = Objects.requireNonNull(worldRegistry, "worldRegistry");
    }

    public net.minecraft.world.entity.Entity handle() {
        return handle;
    }

    @Override
    public UUID uniqueId() {
        return handle.getUUID();
    }

    @Override
    public int entityId() {
        return handle.getId();
    }

    @Override
    public EntityType type() {
        return FandEntityType.of(handle.getType());
    }

    @Override
    public boolean alive() {
        return handle.isAlive();
    }

    @Override
    public Location location() {
        return new Location(world(), handle.getX(), handle.getY(), handle.getZ(), handle.getYRot(), handle.getXRot());
    }

    @Override
    public World world() {
        if (handle.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return worldRegistry.wrap(serverLevel);
        }
        throw new IllegalStateException("Entity is not in a server level: " + handle);
    }

    @Override
    public Vector3 velocity() {
        var movement = handle.getDeltaMovement();
        return new Vector3(movement.x, movement.y, movement.z);
    }

    @Override
    public void setVelocity(Vector3 velocity) {
        Objects.requireNonNull(velocity, "velocity");
        runOnServerThread(() -> handle.setDeltaMovement(new Vec3(velocity.x(), velocity.y(), velocity.z())));
    }

    @Override
    public Optional<Component> customName() {
        return EntityStates.customName(handle);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        runOnServerThread(() -> EntityStates.setCustomName(handle, name));
    }

    @Override
    public boolean customNameVisible() {
        return EntityStates.customNameVisible(handle);
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        runOnServerThread(() -> EntityStates.setCustomNameVisible(handle, visible));
    }

    @Override
    public boolean glowing() {
        return EntityStates.glowing(handle);
    }

    @Override
    public void setGlowing(boolean glowing) {
        runOnServerThread(() -> EntityStates.setGlowing(handle, glowing));
    }

    @Override
    public boolean silent() {
        return EntityStates.silent(handle);
    }

    @Override
    public void setSilent(boolean silent) {
        runOnServerThread(() -> EntityStates.setSilent(handle, silent));
    }

    @Override
    public boolean gravity() {
        return EntityStates.gravity(handle);
    }

    @Override
    public void setGravity(boolean gravity) {
        runOnServerThread(() -> EntityStates.setGravity(handle, gravity));
    }

    @Override
    public boolean invulnerable() {
        return EntityStates.invulnerable(handle);
    }

    @Override
    public void setInvulnerable(boolean invulnerable) {
        runOnServerThread(() -> EntityStates.setInvulnerable(handle, invulnerable));
    }

    @Override
    public Set<String> scoreboardTags() {
        return EntityStates.scoreboardTags(handle);
    }

    @Override
    public void addScoreboardTag(String tag) {
        Objects.requireNonNull(tag, "tag");
        runOnServerThread(() -> EntityStates.addScoreboardTag(handle, tag));
    }

    @Override
    public void removeScoreboardTag(String tag) {
        Objects.requireNonNull(tag, "tag");
        runOnServerThread(() -> EntityStates.removeScoreboardTag(handle, tag));
    }

    @Override
    public double width() {
        return EntityStates.width(handle);
    }

    @Override
    public double height() {
        return EntityStates.height(handle);
    }

    @Override
    public CompletableFuture<Boolean> teleport(Location destination) {
        Objects.requireNonNull(destination, "destination");
        var server = handle.level().getServer();
        if (server == null) {
            return CompletableFuture.completedFuture(false);
        }
        ServerLevel target;
        try {
            target = resolveLevel(destination.world());
        } catch (IllegalArgumentException failure) {
            return CompletableFuture.failedFuture(failure);
        }
        var future = new CompletableFuture<Boolean>();
        Runnable run = () -> {
            if (!alive()) {
                future.complete(false);
                return;
            }
            try {
                boolean ok = handle.teleportTo(
                        target,
                        destination.x(),
                        destination.y(),
                        destination.z(),
                        java.util.Set.of(),
                        destination.yaw(),
                        destination.pitch(),
                        true
                );
                future.complete(ok);
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        };
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
        return future;
    }

    @Override
    public void remove() {
        runOnServerThread(handle::discard);
    }

    @Override
    public Optional<? extends io.fand.api.entity.Entity> vehicle() {
        return Optional.ofNullable(handle.getVehicle()).map(this::wrapRelated);
    }

    @Override
    public List<? extends io.fand.api.entity.Entity> passengers() {
        return handle.getPassengers().stream().map(this::wrapRelated).toList();
    }

    @Override
    public CompletableFuture<Boolean> mount(io.fand.api.entity.Entity vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        return runOnServerThreadFuture(() -> handle.startRiding(EntityHandles.unwrap(vehicle)));
    }

    @Override
    public CompletableFuture<Boolean> addPassenger(io.fand.api.entity.Entity passenger) {
        Objects.requireNonNull(passenger, "passenger");
        return runOnServerThreadFuture(() -> EntityHandles.unwrap(passenger).startRiding(handle));
    }

    @Override
    public CompletableFuture<Boolean> removePassenger(io.fand.api.entity.Entity passenger) {
        Objects.requireNonNull(passenger, "passenger");
        return runOnServerThreadFuture(() -> {
            var passengerHandle = EntityHandles.unwrap(passenger);
            if (passengerHandle.getVehicle() != handle) {
                return false;
            }
            passengerHandle.stopRiding();
            return passengerHandle.getVehicle() != handle;
        });
    }

    @Override
    public CompletableFuture<Boolean> dismount() {
        return runOnServerThreadFuture(() -> {
            var vehicle = handle.getVehicle();
            if (vehicle == null) {
                return false;
            }
            handle.stopRiding();
            return handle.getVehicle() != vehicle;
        });
    }

    @Override
    public void ejectPassengers() {
        runOnServerThread(handle::ejectPassengers);
    }

    @Override
    public boolean onGround() {
        return handle.onGround();
    }

    @Override
    public boolean inWater() {
        return handle.isInWater();
    }

    @Override
    public boolean inLava() {
        return handle.isInLava();
    }

    @Override
    public int fireTicks() {
        return handle.getRemainingFireTicks();
    }

    @Override
    public void setFireTicks(int ticks) {
        runOnServerThread(() -> handle.setRemainingFireTicks(Math.max(0, ticks)));
    }

    @Override
    public int ticksLived() {
        return handle.tickCount;
    }

    @Override
    public DataComponentContainer components() {
        var server = handle.level().getServer();
        if (server == null) {
            throw new IllegalStateException("Entity is not attached to a server: " + handle);
        }
        if (!server.isSameThread()) {
            throw new IllegalStateException("Entity.components() must be accessed on the server thread");
        }
        return EntityComponentStorage.container(server, handle.getUUID());
    }

    protected void runOnServerThread(Runnable task) {
        var server = handle.level().getServer();
        if (server == null || server.isSameThread()) {
            task.run();
        } else {
            server.executeIfPossible(task);
        }
    }

    protected <T> CompletableFuture<T> runOnServerThreadFuture(Supplier<T> task) {
        var server = handle.level().getServer();
        if (server == null || server.isSameThread()) {
            try {
                return CompletableFuture.completedFuture(task.get());
            } catch (Throwable failure) {
                return CompletableFuture.failedFuture(failure);
            }
        }
        var future = new CompletableFuture<T>();
        server.executeIfPossible(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    private ServerLevel resolveLevel(World world) {
        if (world instanceof FandWorld fandWorld) {
            return fandWorld.handle();
        }
        var key = world.key();
        var server = handle.level().getServer();
        if (server != null) {
            for (var level : server.getAllLevels()) {
                var identifier = level.dimension().identifier();
                if (identifier.getNamespace().equals(key.namespace()) && identifier.getPath().equals(key.value())) {
                    return level;
                }
            }
        }
        throw new IllegalArgumentException("World not loaded: " + key.asString());
    }

    private io.fand.api.entity.Entity wrapRelated(net.minecraft.world.entity.Entity entity) {
        return worldRegistry.entityRegistry().wrap(entity);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof io.fand.api.entity.Entity that && this.uniqueId().equals(that.uniqueId());
    }

    @Override
    public int hashCode() {
        return uniqueId().hashCode();
    }
}
