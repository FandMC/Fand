package io.fand.server.entity;

import io.fand.api.entity.GameMode;
import io.fand.api.entity.Player;
import io.fand.api.permission.PermissionService;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.server.audience.BossBarTracker;
import io.fand.server.audience.PacketAudience;
import io.fand.server.command.AdventureBridge;
import io.fand.server.inventory.FandPlayerInventory;
import io.fand.server.world.FandWorld;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

public final class FandPlayer implements Player {

    private volatile ServerPlayer handle;
    private final PermissionService permissions;
    private final PlayerRegistry registry;
    private volatile FandPlayerInventory inventory;
    private final BossBarTracker bossBars;

    public FandPlayer(ServerPlayer handle, PermissionService permissions, PlayerRegistry registry) {
        this.handle = handle;
        this.permissions = permissions;
        this.registry = registry;
        this.inventory = new FandPlayerInventory(handle.getInventory());
        this.bossBars = new BossBarTracker(handle);
    }

    public ServerPlayer handle() {
        return handle;
    }

    void refreshHandle(ServerPlayer newHandle) {
        this.handle = newHandle;
        this.inventory = new FandPlayerInventory(newHandle.getInventory());
        bossBars.rebind(newHandle);
    }

    public void clearTransientState() {
        bossBars.clear();
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
    public Key type() {
        var identifier = EntityType.getKey(handle.getType());
        return Key.key(identifier.getNamespace(), identifier.getPath());
    }

    @Override
    public boolean alive() {
        return online() && handle.isAlive();
    }

    @Override
    public double health() {
        return handle.getHealth();
    }

    @Override
    public double maxHealth() {
        return handle.getMaxHealth();
    }

    @Override
    public void setHealth(double health) {
        var server = handle.level().getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> {
            float clamped = (float) Math.max(0.0, Math.min(health, handle.getMaxHealth()));
            handle.setHealth(clamped);
        };
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
    }

    @Override
    public boolean online() {
        return !handle.hasDisconnected();
    }

    @Override
    public void kick(Component reason) {
        if (handle.connection != null) {
            handle.connection.disconnect(AdventureBridge.toVanilla(reason, handle.registryAccess()));
        }
    }

    @Override
    public Location location() {
        var world = registry.wrapLevel(handle.level());
        return new Location(world, handle.getX(), handle.getY(), handle.getZ(), handle.getYRot(), handle.getXRot());
    }

    @Override
    public World world() {
        return registry.wrapLevel(handle.level());
    }

    @Override
    public CompletableFuture<Boolean> teleport(Location destination) {
        var server = handle.level().getServer();
        if (server == null) {
            return CompletableFuture.completedFuture(false);
        }
        ServerLevel target;
        try {
            target = resolveLevel(destination.world(), server);
        } catch (IllegalArgumentException failure) {
            return CompletableFuture.failedFuture(failure);
        }
        var future = new CompletableFuture<Boolean>();
        Runnable run = () -> {
            if (!online()) {
                future.complete(false);
                return;
            }
            try {
                var ok = handle.teleportTo(
                        target,
                        destination.x(),
                        destination.y(),
                        destination.z(),
                        Set.of(),
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

    private static ServerLevel resolveLevel(World world, net.minecraft.server.MinecraftServer server) {
        if (world instanceof FandWorld fand) {
            return fand.handle();
        }
        var key = world.key();
        for (var level : server.getAllLevels()) {
            var identifier = level.dimension().identifier();
            if (identifier.getNamespace().equals(key.namespace()) && identifier.getPath().equals(key.value())) {
                return level;
            }
        }
        throw new IllegalArgumentException("World not loaded: " + key.asString());
    }

    @Override
    public String name() {
        return handle.getGameProfile().name();
    }

    @Override
    public void sendMessage(Component message) {
        handle.sendSystemMessage(AdventureBridge.toVanilla(message, handle.registryAccess()));
    }

    @Override
    public void sendActionBar(Component message) {
        PacketAudience.sendActionBar(handle, message);
    }

    @Override
    public void showTitle(Title title) {
        PacketAudience.showTitle(handle, title);
    }

    @Override
    public <T> void sendTitlePart(TitlePart<T> part, T value) {
        PacketAudience.sendTitlePart(handle, part, value);
    }

    @Override
    public void clearTitle() {
        PacketAudience.clearTitle(handle);
    }

    @Override
    public void resetTitle() {
        PacketAudience.resetTitle(handle);
    }

    @Override
    public void playSound(Sound sound) {
        PacketAudience.playSound(handle, sound);
    }

    @Override
    public void playSound(Sound sound, double x, double y, double z) {
        PacketAudience.playSoundAt(handle, sound, x, y, z);
    }

    @Override
    public void playSound(Sound sound, Sound.Emitter emitter) {
        if (emitter == Sound.Emitter.self()) {
            PacketAudience.playSoundAt(handle, sound, handle.getX(), handle.getY(), handle.getZ());
        } else {
            PacketAudience.playSound(handle, sound);
        }
    }

    @Override
    public void stopSound(SoundStop stop) {
        PacketAudience.stopSound(handle, stop);
    }

    @Override
    public void showBossBar(BossBar bar) {
        runOnMain(() -> bossBars.show(bar));
    }

    @Override
    public void hideBossBar(BossBar bar) {
        runOnMain(() -> bossBars.hide(bar));
    }

    private void runOnMain(Runnable task) {
        var server = handle.level().getServer();
        if (server == null) {
            return;
        }
        if (server.isSameThread()) {
            task.run();
        } else {
            server.executeIfPossible(task);
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions.hasPermission(this, permission);
    }

    @Override
    public boolean operator() {
        var server = handle.level().getServer();
        return server != null && server.getPlayerList().isOp(handle.nameAndId());
    }

    @Override
    public Optional<Boolean> permissionValue(String node) {
        return Optional.empty();
    }

    @Override
    public io.fand.api.inventory.PlayerInventory inventory() {
        return inventory;
    }

    @Override
    public GameMode gameMode() {
        return GameModes.toApi(handle.gameMode.getGameModeForPlayer());
    }

    @Override
    public void setGameMode(GameMode mode) {
        var vanilla = GameModes.toVanilla(mode);
        runOnMain(() -> handle.setGameMode(vanilla));
    }

    @Override
    public int foodLevel() {
        return handle.getFoodData().getFoodLevel();
    }

    @Override
    public void setFoodLevel(int level) {
        int clamped = Math.max(0, Math.min(20, level));
        runOnMain(() -> handle.getFoodData().setFoodLevel(clamped));
    }

    @Override
    public float saturation() {
        return handle.getFoodData().getSaturationLevel();
    }

    @Override
    public void setSaturation(float saturation) {
        runOnMain(() -> handle.getFoodData().setSaturation(saturation));
    }

    @Override
    public int experienceLevel() {
        return handle.experienceLevel;
    }

    @Override
    public void setExperienceLevel(int level) {
        runOnMain(() -> handle.setExperienceLevels(level));
    }

    @Override
    public float experienceProgress() {
        return handle.experienceProgress;
    }

    @Override
    public void setExperienceProgress(float progress) {
        float clamped = Math.max(0.0F, Math.min(0.9999F, progress));
        runOnMain(() -> {
            handle.experienceProgress = clamped;
            handle.resetSentInfo();
        });
    }

    @Override
    public void giveExperience(int points) {
        if (points == 0) {
            return;
        }
        runOnMain(() -> handle.giveExperiencePoints(points));
    }

    @Override
    public boolean flying() {
        return handle.getAbilities().flying;
    }

    @Override
    public void setFlying(boolean flying) {
        runOnMain(() -> {
            var abilities = handle.getAbilities();
            if (flying && !abilities.mayfly) {
                pushAbilities();
                return;
            }
            if (abilities.flying != flying) {
                abilities.flying = flying;
                pushAbilities();
            }
        });
    }

    @Override
    public boolean allowFlight() {
        return handle.getAbilities().mayfly;
    }

    @Override
    public void setAllowFlight(boolean allow) {
        runOnMain(() -> {
            var abilities = handle.getAbilities();
            if (abilities.mayfly == allow) {
                return;
            }
            abilities.mayfly = allow;
            if (!allow) {
                abilities.flying = false;
            }
            pushAbilities();
        });
    }

    private void pushAbilities() {
        if (handle.connection != null) {
            handle.connection.send(new ClientboundPlayerAbilitiesPacket(handle.getAbilities()));
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandPlayer that && this.uniqueId().equals(that.uniqueId());
    }

    @Override
    public int hashCode() {
        return uniqueId().hashCode();
    }

    @Override
    public String toString() {
        return "FandPlayer(" + name() + ")";
    }
}
