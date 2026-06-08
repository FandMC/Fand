package io.fand.server.entity;

import io.fand.api.component.DataComponentContainer;
import io.fand.api.entity.ClientSettings;
import io.fand.api.entity.EntityType;
import io.fand.api.entity.EntityEffect;
import io.fand.api.entity.GameMode;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;
import io.fand.api.item.component.ItemEquipmentSlot;
import io.fand.api.permission.PermissionService;
import io.fand.api.scoreboard.Sidebar;
import io.fand.api.world.Location;
import io.fand.api.world.Vector3;
import io.fand.api.world.World;
import io.fand.api.world.particle.ParticleEffect;
import io.fand.api.world.particle.ParticleEmission;
import io.fand.api.world.sound.SoundEffect;
import io.fand.server.audience.BossBarTracker;
import io.fand.server.audience.PacketAudience;
import io.fand.server.audience.SidebarTracker;
import io.fand.server.command.AdventureBridge;
import io.fand.server.component.EntityComponentStorage;
import io.fand.server.inventory.FandPlayerInventory;
import io.fand.server.item.FandItemStacks;
import io.fand.server.world.ParticleEffects;
import io.fand.server.world.FandWorld;
import io.fand.server.world.SoundEffects;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class FandPlayer implements Player {

    private volatile Bound bound;
    private final PermissionService permissions;
    private final PlayerRegistry registry;
    private final BossBarTracker bossBars;
    private final SidebarTracker sidebar;

    public FandPlayer(ServerPlayer handle, PermissionService permissions, PlayerRegistry registry) {
        this.bound = new Bound(handle, new FandPlayerInventory(handle.getInventory()));
        this.permissions = permissions;
        this.registry = registry;
        this.bossBars = new BossBarTracker(handle);
        this.sidebar = new SidebarTracker(handle);
    }

    public ServerPlayer handle() {
        return bound.handle;
    }

    void refreshHandle(ServerPlayer newHandle) {
        this.bound = new Bound(newHandle, new FandPlayerInventory(newHandle.getInventory()));
        bossBars.rebind(newHandle);
        sidebar.rebind(newHandle);
    }

    public void clearTransientState() {
        bossBars.clear();
        sidebar.clear();
    }

    private record Bound(ServerPlayer handle, FandPlayerInventory inventory) {
    }

    @Override
    public UUID uniqueId() {
        return bound.handle.getUUID();
    }

    @Override
    public int entityId() {
        return bound.handle.getId();
    }

    @Override
    public EntityType type() {
        return FandEntityType.of(bound.handle.getType());
    }

    @Override
    public boolean alive() {
        return online() && bound.handle.isAlive();
    }

    @Override
    public double health() {
        return bound.handle.getHealth();
    }

    @Override
    public double maxHealth() {
        return bound.handle.getMaxHealth();
    }

    @Override
    public void setHealth(double health) {
        var handle = bound.handle;
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
    public boolean dead() {
        return bound.handle.isDeadOrDying();
    }

    @Override
    public void damage(double amount) {
        runOnServerThread(() -> {
            var handle = bound.handle;
            if (handle.level() instanceof ServerLevel level) {
                handle.hurtServer(level, handle.damageSources().generic(), (float) Math.max(0.0, amount));
            }
        });
    }

    @Override
    public void damage(double amount, io.fand.api.entity.Entity source) {
        Objects.requireNonNull(source, "source");
        runOnServerThread(() -> {
            var handle = bound.handle;
            if (!(handle.level() instanceof ServerLevel level)) {
                return;
            }
            var sourceHandle = EntityHandles.unwrap(source);
            var damageSource = sourceHandle instanceof net.minecraft.world.entity.LivingEntity living
                    ? handle.damageSources().mobAttack(living)
                    : handle.damageSources().generic();
            handle.hurtServer(level, damageSource, (float) Math.max(0.0, amount));
        });
    }

    @Override
    public void heal(double amount) {
        if (amount <= 0.0) {
            return;
        }
        runOnServerThread(() -> bound.handle.heal((float) amount));
    }

    @Override
    public double absorption() {
        return bound.handle.getAbsorptionAmount();
    }

    @Override
    public void setAbsorption(double absorption) {
        runOnServerThread(() -> bound.handle.setAbsorptionAmount((float) Math.max(0.0, absorption)));
    }

    @Override
    public int armor() {
        return bound.handle.getArmorValue();
    }

    @Override
    public Optional<? extends io.fand.api.entity.Attribute> attribute(Key key) {
        Objects.requireNonNull(key, "key");
        return EntityAttributes.holder(key)
                .map(bound.handle::getAttribute)
                .map(attribute -> new FandAttribute(attribute, this::runOnServerThread));
    }

    @Override
    public Collection<EntityEffect> effects() {
        return bound.handle.getActiveEffects().stream()
                .map(EntityEffects::toApi)
                .toList();
    }

    @Override
    public Optional<EntityEffect> effect(Key key) {
        Objects.requireNonNull(key, "key");
        return EntityEffects.holder(key)
                .map(bound.handle::getEffect)
                .map(EntityEffects::toApi);
    }

    @Override
    public void addEffect(EntityEffect effect) {
        Objects.requireNonNull(effect, "effect");
        runOnServerThread(() -> bound.handle.addEffect(EntityEffects.toVanilla(effect)));
    }

    @Override
    public void removeEffect(Key key) {
        Objects.requireNonNull(key, "key");
        EntityEffects.holder(key).ifPresent(holder -> runOnServerThread(() -> bound.handle.removeEffect(holder)));
    }

    @Override
    public ItemStack equipment(ItemEquipmentSlot slot) {
        Objects.requireNonNull(slot, "slot");
        return FandItemStacks.fromVanilla(bound.handle.getItemBySlot(EquipmentSlots.toVanilla(slot)));
    }

    @Override
    public void setEquipment(ItemEquipmentSlot slot, ItemStack item) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(item, "item");
        runOnServerThread(() -> bound.handle.setItemSlot(
                EquipmentSlots.toVanilla(slot),
                FandItemStacks.toVanilla(item)));
    }

    @Override
    public boolean online() {
        return !bound.handle.hasDisconnected();
    }

    @Override
    public int ping() {
        var connection = bound.handle.connection;
        return connection == null ? 0 : connection.latency();
    }

    @Override
    public ClientSettings clientSettings() {
        var information = bound.handle.clientInformation();
        return new ClientSettings(information.language(), information.viewDistance());
    }

    @Override
    public void kick(Component reason) {
        Objects.requireNonNull(reason, "reason");
        runOnServerThread(() -> {
            var handle = bound.handle;
            if (handle.connection != null) {
                handle.connection.disconnect(AdventureBridge.toVanilla(reason, handle.registryAccess()));
            }
        });
    }

    @Override
    public void playSound(SoundEffect sound) {
        Objects.requireNonNull(sound, "sound");
        runOnServerThread(() -> {
            var handle = bound.handle;
            if (online()) {
                SoundEffects.playTo(handle, location(), sound);
            }
        });
    }

    @Override
    public void playSound(Location location, SoundEffect sound) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(sound, "sound");
        runOnServerThread(() -> {
            var handle = bound.handle;
            if (online() && sameWorld(location, handle.level())) {
                SoundEffects.playTo(handle, location, sound);
            }
        });
    }

    @Override
    public void spawnParticle(Location location, ParticleEffect effect, ParticleEmission emission) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(emission, "emission");
        runOnServerThread(() -> {
            var handle = bound.handle;
            if (online() && sameWorld(location, handle.level())) {
                ParticleEffects.spawnTo(handle, location, effect, emission);
            }
        });
    }

    @Override
    public Location location() {
        var handle = bound.handle;
        var world = registry.wrapLevel(handle.level());
        return new Location(world, handle.getX(), handle.getY(), handle.getZ(), handle.getYRot(), handle.getXRot());
    }

    @Override
    public World world() {
        return registry.wrapLevel(bound.handle.level());
    }

    @Override
    public Vector3 velocity() {
        var movement = bound.handle.getDeltaMovement();
        return new Vector3(movement.x, movement.y, movement.z);
    }

    @Override
    public void setVelocity(Vector3 velocity) {
        Objects.requireNonNull(velocity, "velocity");
        runOnServerThread(() -> bound.handle.setDeltaMovement(new Vec3(velocity.x(), velocity.y(), velocity.z())));
    }

    @Override
    public Optional<Component> customName() {
        return EntityStates.customName(bound.handle);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        runOnServerThread(() -> EntityStates.setCustomName(bound.handle, name));
    }

    @Override
    public boolean customNameVisible() {
        return EntityStates.customNameVisible(bound.handle);
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        runOnServerThread(() -> EntityStates.setCustomNameVisible(bound.handle, visible));
    }

    @Override
    public boolean glowing() {
        return EntityStates.glowing(bound.handle);
    }

    @Override
    public void setGlowing(boolean glowing) {
        runOnServerThread(() -> EntityStates.setGlowing(bound.handle, glowing));
    }

    @Override
    public boolean silent() {
        return EntityStates.silent(bound.handle);
    }

    @Override
    public void setSilent(boolean silent) {
        runOnServerThread(() -> EntityStates.setSilent(bound.handle, silent));
    }

    @Override
    public boolean gravity() {
        return EntityStates.gravity(bound.handle);
    }

    @Override
    public void setGravity(boolean gravity) {
        runOnServerThread(() -> EntityStates.setGravity(bound.handle, gravity));
    }

    @Override
    public boolean invulnerable() {
        return EntityStates.invulnerable(bound.handle);
    }

    @Override
    public void setInvulnerable(boolean invulnerable) {
        runOnServerThread(() -> EntityStates.setInvulnerable(bound.handle, invulnerable));
    }

    @Override
    public Set<String> scoreboardTags() {
        return EntityStates.scoreboardTags(bound.handle);
    }

    @Override
    public void addScoreboardTag(String tag) {
        Objects.requireNonNull(tag, "tag");
        runOnServerThread(() -> EntityStates.addScoreboardTag(bound.handle, tag));
    }

    @Override
    public void removeScoreboardTag(String tag) {
        Objects.requireNonNull(tag, "tag");
        runOnServerThread(() -> EntityStates.removeScoreboardTag(bound.handle, tag));
    }

    @Override
    public double width() {
        return EntityStates.width(bound.handle);
    }

    @Override
    public double height() {
        return EntityStates.height(bound.handle);
    }

    @Override
    public void remove() {
        kick(Component.text("Disconnected"));
    }

    @Override
    public Optional<? extends io.fand.api.entity.Entity> vehicle() {
        var vehicle = bound.handle.getVehicle();
        if (vehicle == null) {
            return Optional.empty();
        }
        var worldRegistry = registry.worldRegistry();
        if (worldRegistry == null) {
            return Optional.empty();
        }
        return Optional.of(worldRegistry.entityRegistry().wrap(vehicle));
    }

    @Override
    public java.util.List<? extends io.fand.api.entity.Entity> passengers() {
        var worldRegistry = registry.worldRegistry();
        if (worldRegistry == null) {
            return java.util.List.of();
        }
        return bound.handle.getPassengers().stream()
                .map(worldRegistry.entityRegistry()::wrap)
                .toList();
    }

    @Override
    public CompletableFuture<Boolean> mount(io.fand.api.entity.Entity vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        return runOnServerThreadFuture(() -> bound.handle.startRiding(EntityHandles.unwrap(vehicle)));
    }

    @Override
    public CompletableFuture<Boolean> addPassenger(io.fand.api.entity.Entity passenger) {
        Objects.requireNonNull(passenger, "passenger");
        return runOnServerThreadFuture(() -> EntityHandles.unwrap(passenger).startRiding(bound.handle));
    }

    @Override
    public CompletableFuture<Boolean> removePassenger(io.fand.api.entity.Entity passenger) {
        Objects.requireNonNull(passenger, "passenger");
        return runOnServerThreadFuture(() -> {
            var passengerHandle = EntityHandles.unwrap(passenger);
            var handle = bound.handle;
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
            var handle = bound.handle;
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
        runOnServerThread(() -> bound.handle.ejectPassengers());
    }

    @Override
    public boolean onGround() {
        return bound.handle.onGround();
    }

    @Override
    public boolean inWater() {
        return bound.handle.isInWater();
    }

    @Override
    public boolean inLava() {
        return bound.handle.isInLava();
    }

    @Override
    public int fireTicks() {
        return bound.handle.getRemainingFireTicks();
    }

    @Override
    public void setFireTicks(int ticks) {
        runOnServerThread(() -> bound.handle.setRemainingFireTicks(Math.max(0, ticks)));
    }

    @Override
    public int ticksLived() {
        return bound.handle.tickCount;
    }

    @Override
    public DataComponentContainer components() {
        var handle = bound.handle;
        var server = handle.level().getServer();
        if (server == null) {
            throw new IllegalStateException("Player is not attached to a server: " + handle);
        }
        if (!server.isSameThread()) {
            throw new IllegalStateException("Player.components() must be accessed on the server thread");
        }
        return EntityComponentStorage.container(server, handle.getUUID());
    }

    @Override
    public CompletableFuture<Boolean> teleport(Location destination) {
        var handle = bound.handle;
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
                var ok = io.fand.server.event.PlayerEvents.withTeleportCause(
                        io.fand.api.event.player.PlayerTeleportEvent.Cause.PLUGIN,
                        () -> handle.teleportTo(
                                target,
                                destination.x(),
                                destination.y(),
                                destination.z(),
                                Set.of(),
                                destination.yaw(),
                                destination.pitch(),
                                true
                        )
                );
                if (ok) {
                    registry.refreshSnapshots();
                }
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

    private static boolean sameWorld(Location location, ServerLevel level) {
        var identifier = level.dimension().identifier();
        var key = location.world().key();
        return identifier.getNamespace().equals(key.namespace()) && identifier.getPath().equals(key.value());
    }

    @Override
    public String name() {
        return bound.handle.getGameProfile().name();
    }

    @Override
    public void sendMessage(Component message) {
        Objects.requireNonNull(message, "message");
        runOnServerThread(() -> {
            var handle = bound.handle;
            handle.sendSystemMessage(AdventureBridge.toVanilla(message, handle.registryAccess()));
        });
    }

    @Override
    public void sendActionBar(Component message) {
        Objects.requireNonNull(message, "message");
        runOnServerThread(() -> PacketAudience.sendActionBar(bound.handle, message));
    }

    @Override
    public void sendTabList(Component header, Component footer) {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(footer, "footer");
        runOnServerThread(() -> PacketAudience.sendTabList(bound.handle, header, footer));
    }

    @Override
    public void clearTabList() {
        runOnServerThread(() -> PacketAudience.clearTabList(bound.handle));
    }

    @Override
    public void sendPlayerListHeader(Component header) {
        sendTabList(header, Component.empty());
    }

    @Override
    public void sendPlayerListFooter(Component footer) {
        sendTabList(Component.empty(), footer);
    }

    @Override
    public void sendPlayerListHeaderAndFooter(Component header, Component footer) {
        sendTabList(header, footer);
    }

    @Override
    public void showTitle(Title title) {
        Objects.requireNonNull(title, "title");
        runOnServerThread(() -> PacketAudience.showTitle(bound.handle, title));
    }

    @Override
    public <T> void sendTitlePart(TitlePart<T> part, T value) {
        Objects.requireNonNull(part, "part");
        Objects.requireNonNull(value, "value");
        runOnServerThread(() -> PacketAudience.sendTitlePart(bound.handle, part, value));
    }

    @Override
    public void clearTitle() {
        runOnServerThread(() -> PacketAudience.clearTitle(bound.handle));
    }

    @Override
    public void resetTitle() {
        runOnServerThread(() -> PacketAudience.resetTitle(bound.handle));
    }

    @Override
    public void playSound(Sound sound) {
        Objects.requireNonNull(sound, "sound");
        runOnServerThread(() -> PacketAudience.playSound(bound.handle, sound));
    }

    @Override
    public void playSound(Sound sound, double x, double y, double z) {
        Objects.requireNonNull(sound, "sound");
        runOnServerThread(() -> PacketAudience.playSoundAt(bound.handle, sound, x, y, z));
    }

    @Override
    public void playSound(Sound sound, Sound.Emitter emitter) {
        Objects.requireNonNull(sound, "sound");
        Objects.requireNonNull(emitter, "emitter");
        runOnServerThread(() -> {
            var handle = bound.handle;
            if (emitter == Sound.Emitter.self()) {
                PacketAudience.playSoundAt(handle, sound, handle.getX(), handle.getY(), handle.getZ());
            } else {
                PacketAudience.playSound(handle, sound);
            }
        });
    }

    @Override
    public void stopSound(SoundStop stop) {
        Objects.requireNonNull(stop, "stop");
        runOnServerThread(() -> PacketAudience.stopSound(bound.handle, stop));
    }

    @Override
    public void showBossBar(BossBar bar) {
        Objects.requireNonNull(bar, "bar");
        runOnServerThread(() -> bossBars.show(bar));
    }

    @Override
    public void hideBossBar(BossBar bar) {
        Objects.requireNonNull(bar, "bar");
        runOnServerThread(() -> bossBars.hide(bar));
    }

    @Override
    public void showSidebar(Sidebar view) {
        Objects.requireNonNull(view, "view");
        runOnServerThread(() -> sidebar.show(view));
    }

    @Override
    public void clearSidebar() {
        runOnServerThread(sidebar::clear);
    }

    private void runOnServerThread(Runnable task) {
        var server = bound.handle.level().getServer();
        if (server == null) {
            return;
        }
        if (server.isSameThread()) {
            task.run();
        } else {
            server.executeIfPossible(task);
        }
    }

    private <T> CompletableFuture<T> runOnServerThreadFuture(Supplier<T> task) {
        var server = bound.handle.level().getServer();
        if (server == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Player is not attached to a server"));
        }
        if (server.isSameThread()) {
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

    @Override
    public boolean hasPermission(String permission) {
        return permissions.hasPermission(this, permission);
    }

    @Override
    public boolean operator() {
        var handle = bound.handle;
        var server = handle.level().getServer();
        return server != null && server.getPlayerList().isOp(handle.nameAndId());
    }

    @Override
    public Optional<Boolean> permissionValue(String node) {
        return Optional.empty();
    }

    @Override
    public io.fand.api.inventory.PlayerInventory inventory() {
        return bound.inventory;
    }

    @Override
    public GameMode gameMode() {
        return GameModes.toApi(bound.handle.gameMode.getGameModeForPlayer());
    }

    @Override
    public void setGameMode(GameMode mode) {
        var vanilla = GameModes.toVanilla(mode);
        runOnServerThread(() -> bound.handle.setGameMode(vanilla));
    }

    @Override
    public int foodLevel() {
        return bound.handle.getFoodData().getFoodLevel();
    }

    @Override
    public void setFoodLevel(int level) {
        int clamped = Math.max(0, Math.min(20, level));
        runOnServerThread(() -> bound.handle.getFoodData().setFoodLevel(clamped));
    }

    @Override
    public float saturation() {
        return bound.handle.getFoodData().getSaturationLevel();
    }

    @Override
    public void setSaturation(float saturation) {
        runOnServerThread(() -> bound.handle.getFoodData().setSaturation(saturation));
    }

    @Override
    public int experienceLevel() {
        return bound.handle.experienceLevel;
    }

    @Override
    public void setExperienceLevel(int level) {
        runOnServerThread(() -> bound.handle.setExperienceLevels(level));
    }

    @Override
    public float experienceProgress() {
        return bound.handle.experienceProgress;
    }

    @Override
    public void setExperienceProgress(float progress) {
        float clamped = Math.max(0.0F, Math.min(0.9999F, progress));
        runOnServerThread(() -> {
            var handle = bound.handle;
            handle.experienceProgress = clamped;
            handle.resetSentInfo();
        });
    }

    @Override
    public void giveExperience(int points) {
        if (points == 0) {
            return;
        }
        runOnServerThread(() -> bound.handle.giveExperiencePoints(points));
    }

    @Override
    public boolean flying() {
        return bound.handle.getAbilities().flying;
    }

    @Override
    public void setFlying(boolean flying) {
        runOnServerThread(() -> {
            var abilities = bound.handle.getAbilities();
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
        return bound.handle.getAbilities().mayfly;
    }

    @Override
    public void setAllowFlight(boolean allow) {
        runOnServerThread(() -> {
            var abilities = bound.handle.getAbilities();
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

    @Override
    public CompletableFuture<Optional<io.fand.api.inventory.Inventory>> openInventory(
            io.fand.api.inventory.InventoryType type, int size) {
        Objects.requireNonNull(type, "type");
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0, got " + size);
        }
        if (type == io.fand.api.inventory.InventoryType.PLAYER
                || type == io.fand.api.inventory.InventoryType.UNKNOWN) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        var built = io.fand.server.inventory.OpenableContainers.build(type, size);
        if (built == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        var handle = bound.handle;
        var server = handle.level().getServer();
        if (server == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        var future = new CompletableFuture<Optional<io.fand.api.inventory.Inventory>>();
        Runnable run = () -> {
            if (!online()) {
                future.complete(Optional.empty());
                return;
            }
            try {
                var slot = handle.openMenu(built.provider());
                if (slot.isPresent()) {
                    future.complete(Optional.of(
                            new io.fand.server.inventory.FandContainerInventory(built.container(), type)));
                } else {
                    future.complete(Optional.empty());
                }
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
    public CompletableFuture<Boolean> openInventory(io.fand.api.inventory.Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        if (!(inventory instanceof io.fand.server.inventory.FandInventory fand)) {
            throw new IllegalArgumentException(
                    "openInventory(Inventory) requires an inventory created via Inventories.create");
        }
        var handle = bound.handle;
        var server = handle.level().getServer();
        if (server == null) {
            return CompletableFuture.completedFuture(false);
        }
        var future = new CompletableFuture<Boolean>();
        Runnable run = () -> {
            if (!online()) {
                future.complete(false);
                return;
            }
            try {
                var vanillaTitle = io.fand.server.command.AdventureBridge.toVanilla(
                        fand.title(), handle.registryAccess());
                var built = io.fand.server.inventory.OpenableContainers.build(
                        fand.type(), fand.size(), fand.container(), vanillaTitle);
                if (built == null) {
                    future.complete(false);
                    return;
                }
                var slot = handle.openMenu(built.provider());
                future.complete(slot.isPresent());
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
    public Optional<io.fand.api.inventory.Inventory> openInventory() {
        var handle = bound.handle;
        var menu = handle.containerMenu;
        if (menu == null || menu == handle.inventoryMenu) {
            return Optional.empty();
        }
        return Optional.of(new io.fand.server.inventory.ContainerMenuView(menu));
    }

    @Override
    public void closeInventory() {
        runOnServerThread(() -> bound.handle.closeContainer());
    }

    private void pushAbilities() {
        var handle = bound.handle;
        if (handle.connection != null) {
            handle.connection.send(new ClientboundPlayerAbilitiesPacket(handle.getAbilities()));
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof io.fand.api.entity.Entity that && this.uniqueId().equals(that.uniqueId());
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
