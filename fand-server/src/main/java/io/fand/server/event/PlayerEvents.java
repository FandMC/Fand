package io.fand.server.event;

import io.fand.api.event.player.PlayerDropItemEvent;
import io.fand.api.event.player.PlayerPickupItemEvent;
import io.fand.api.event.player.PlayerRespawnEvent;
import io.fand.api.event.player.PlayerTeleportEvent;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.server.command.CommandEvents;
import io.fand.server.entity.FandPlayer;
import io.fand.server.hooks.FandHooks;
import io.fand.server.item.FandItemStacks;
import io.fand.server.world.FandWorld;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlayerEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerEvents.class);
    private static final ThreadLocal<PlayerTeleportEvent.Cause> NEXT_TELEPORT_CAUSE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> SUPPRESS_TELEPORT_EVENT = ThreadLocal.withInitial(() -> 0);

    private PlayerEvents() {
    }

    public static <T> T withTeleportCause(PlayerTeleportEvent.Cause cause, java.util.concurrent.Callable<T> task) {
        PlayerTeleportEvent.Cause previous = NEXT_TELEPORT_CAUSE.get();
        NEXT_TELEPORT_CAUSE.set(Objects.requireNonNull(cause, "cause"));
        try {
            return task.call();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new RuntimeException(failure);
        } finally {
            restoreTeleportCause(previous);
        }
    }

    public static void withTeleportCause(PlayerTeleportEvent.Cause cause, Runnable task) {
        PlayerTeleportEvent.Cause previous = NEXT_TELEPORT_CAUSE.get();
        NEXT_TELEPORT_CAUSE.set(Objects.requireNonNull(cause, "cause"));
        try {
            task.run();
        } finally {
            restoreTeleportCause(previous);
        }
    }

    public static <T> T withoutTeleportEvent(java.util.concurrent.Callable<T> task) throws Exception {
        int previous = SUPPRESS_TELEPORT_EVENT.get();
        SUPPRESS_TELEPORT_EVENT.set(previous + 1);
        try {
            return task.call();
        } finally {
            restoreSuppressDepth(previous);
        }
    }

    public static net.minecraft.world.item.ItemStack fireDrop(
            ServerPlayer player,
            net.minecraft.world.item.ItemStack itemStack,
            boolean randomly,
            boolean thrownFromHand) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerDropItemEvent.class)) {
            return itemStack;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return itemStack;
        }
        var event = new PlayerDropItemEvent(
                fandPlayer,
                FandItemStacks.fromVanilla(itemStack),
                randomly,
                thrownFromHand);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerDropItemEvent listener failed", failure);
            return itemStack;
        }
        if (event.cancelled() || event.item().isEmpty()) {
            if (thrownFromHand || randomly) {
                player.getInventory().placeItemBackInInventory(itemStack.copy());
            }
            return null;
        }
        try {
            return FandItemStacks.toVanilla(event.item());
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerDropItemEvent supplied an invalid item stack", failure);
            return itemStack;
        }
    }

    public static boolean firePickup(ServerPlayer player, ItemEntity entity) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerPickupItemEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        var original = entity.getItem();
        var event = new PlayerPickupItemEvent(fandPlayer, FandItemStacks.fromVanilla(original));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerPickupItemEvent listener failed", failure);
            return true;
        }
        if (event.cancelled() || event.item().isEmpty()) {
            return false;
        }
        try {
            var replacement = FandItemStacks.toVanilla(event.item());
            if (!net.minecraft.world.item.ItemStack.matches(original, replacement)) {
                entity.setItem(replacement);
            }
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerPickupItemEvent supplied an invalid item stack", failure);
        }
        return true;
    }

    public static @Nullable TeleportTransition fireTeleport(ServerPlayer player, TeleportTransition transition) {
        var bus = FandHooks.events();
        if (SUPPRESS_TELEPORT_EVENT.get() > 0 || !bus.hasListeners(PlayerTeleportEvent.class)) {
            return transition;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return transition;
        }
        var fromWorld = FandHooks.wrapWorld(player.level());
        var toWorld = FandHooks.wrapWorld(transition.newLevel());
        if (fromWorld == null || toWorld == null) {
            return transition;
        }
        PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(
                PositionMoveRotation.of(player),
                PositionMoveRotation.of(transition),
                transition.relatives());
        var from = new Location(fromWorld, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        var to = new Location(
                toWorld,
                absolute.position().x,
                absolute.position().y,
                absolute.position().z,
                absolute.yRot(),
                absolute.xRot());
        var event = new PlayerTeleportEvent(fandPlayer, from, to, currentTeleportCause());
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerTeleportEvent listener failed", failure);
            return transition;
        }
        if (event.cancelled()) {
            return null;
        }
        if (sameLocation(to, event.to())) {
            return transition;
        }
        ServerLevel level = resolveLevel(event.to().world(), player);
        if (level == null) {
            LOGGER.warn("PlayerTeleportEvent targeted an unloaded world: {}", event.to().world().key().asString());
            return transition;
        }
        return new TeleportTransition(
                level,
                new Vec3(event.to().x(), event.to().y(), event.to().z()),
                transition.deltaMovement(),
                event.to().yaw(),
                event.to().pitch(),
                transition.missingRespawnBlock(),
                transition.asPassenger(),
                java.util.Set.of(),
                transition.postTeleportTransition());
    }

    public static ServerLevel fireRespawn(
            ServerPlayer player,
            ServerLevel currentLevel,
            boolean keepAllPlayerData,
            net.minecraft.world.entity.Entity.RemovalReason removalReason) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerRespawnEvent.class)) {
            return currentLevel;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var world = FandHooks.wrapWorld(currentLevel);
        if (fandPlayer == null || world == null) {
            return currentLevel;
        }
        var location = new Location(world, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        var event = new PlayerRespawnEvent(fandPlayer, location, respawnCause(removalReason), keepAllPlayerData);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerRespawnEvent listener failed", failure);
            return currentLevel;
        }
        if (sameLocation(location, event.respawnLocation())) {
            return currentLevel;
        }
        ServerLevel level = resolveLevel(event.respawnLocation().world(), player);
        if (level == null) {
            LOGGER.warn("PlayerRespawnEvent targeted an unloaded world: {}", event.respawnLocation().world().key().asString());
            return currentLevel;
        }
        if (level != player.level()) {
            player.setServerLevel(level);
        }
        player.snapTo(
                event.respawnLocation().x(),
                event.respawnLocation().y(),
                event.respawnLocation().z(),
                event.respawnLocation().yaw(),
                event.respawnLocation().pitch());
        return level;
    }

    private static PlayerTeleportEvent.Cause currentTeleportCause() {
        PlayerTeleportEvent.Cause explicit = NEXT_TELEPORT_CAUSE.get();
        if (explicit != null) {
            return explicit;
        }
        return CommandEvents.inCommandContext() ? PlayerTeleportEvent.Cause.COMMAND : PlayerTeleportEvent.Cause.UNKNOWN;
    }

    private static void restoreTeleportCause(PlayerTeleportEvent.Cause previous) {
        if (previous == null) {
            NEXT_TELEPORT_CAUSE.remove();
        } else {
            NEXT_TELEPORT_CAUSE.set(previous);
        }
    }

    private static void restoreSuppressDepth(int previous) {
        if (previous == 0) {
            SUPPRESS_TELEPORT_EVENT.remove();
        } else {
            SUPPRESS_TELEPORT_EVENT.set(previous);
        }
    }

    private static PlayerRespawnEvent.Cause respawnCause(net.minecraft.world.entity.Entity.RemovalReason reason) {
        return switch (reason) {
            case KILLED -> PlayerRespawnEvent.Cause.DEATH;
            case CHANGED_DIMENSION -> PlayerRespawnEvent.Cause.DIMENSION_CHANGE;
            default -> PlayerRespawnEvent.Cause.UNKNOWN;
        };
    }

    private static @Nullable ServerLevel resolveLevel(World world, ServerPlayer player) {
        if (world instanceof FandWorld fandWorld) {
            return fandWorld.handle();
        }
        var key = world.key();
        var server = player.level().getServer();
        if (server == null) {
            return null;
        }
        for (var level : server.getAllLevels()) {
            var identifier = level.dimension().identifier();
            if (identifier.getNamespace().equals(key.namespace()) && identifier.getPath().equals(key.value())) {
                return level;
            }
        }
        return null;
    }

    private static boolean sameLocation(Location a, Location b) {
        return a.world().equals(b.world())
                && Double.compare(a.x(), b.x()) == 0
                && Double.compare(a.y(), b.y()) == 0
                && Double.compare(a.z(), b.z()) == 0
                && Float.compare(a.yaw(), b.yaw()) == 0
                && Float.compare(a.pitch(), b.pitch()) == 0;
    }
}
