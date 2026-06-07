package io.fand.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Entity;
import io.fand.api.entity.GameMode;
import io.fand.api.entity.Player;
import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.event.block.BlockChangeEvent;
import io.fand.api.event.block.BlockPhysicsEvent;
import io.fand.api.event.command.CommandExecuteEvent;
import io.fand.api.event.entity.EntityDamageEvent;
import io.fand.api.event.entity.EntityDeathEvent;
import io.fand.api.event.entity.EntityRemoveEvent;
import io.fand.api.event.entity.EntitySpawnEvent;
import io.fand.api.event.entity.EntityTeleportEvent;
import io.fand.api.event.entity.ExplosionPrimeEvent;
import io.fand.api.event.inventory.ClickType;
import io.fand.api.event.inventory.InventoryAction;
import io.fand.api.event.inventory.InventoryClickEvent;
import io.fand.api.event.player.PlayerCommandPreprocessEvent;
import io.fand.api.event.player.PlayerDropItemEvent;
import io.fand.api.event.player.PlayerGameModeChangeEvent;
import io.fand.api.event.player.PlayerInteractEvent;
import io.fand.api.event.player.PlayerItemConsumeEvent;
import io.fand.api.event.player.PlayerItemDamageEvent;
import io.fand.api.event.player.PlayerKickEvent;
import io.fand.api.event.player.PlayerPickupItemEvent;
import io.fand.api.event.player.PlayerRespawnEvent;
import io.fand.api.event.player.PlayerSwapHandItemsEvent;
import io.fand.api.event.player.PlayerTeleportEvent;
import io.fand.api.event.player.PlayerToggleSneakEvent;
import io.fand.api.event.player.PlayerToggleSprintEvent;
import io.fand.api.event.world.ChunkLoadEvent;
import io.fand.api.event.world.ChunkUnloadEvent;
import io.fand.api.event.world.ThunderChangeEvent;
import io.fand.api.event.world.WeatherChangeEvent;
import io.fand.api.event.world.WorldLoadEvent;
import io.fand.api.event.world.WorldSaveEvent;
import io.fand.api.event.world.WorldUnloadEvent;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.world.Difficulty;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.api.world.WorldBorder;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class EventPayloadTest {

    @Test
    void playerInteractEventCarriesHandItem() {
        var player = proxy(Player.class);
        var item = new ItemStack(new TestItemType(Key.key("minecraft:compass"), 64), 1);

        var event = new PlayerInteractEvent(
                player,
                PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
                PlayerInteractEvent.Hand.MAIN_HAND,
                Optional.empty(),
                item);

        assertThat(event.player()).isSameAs(player);
        assertThat(event.item()).isSameAs(item);
        assertThat(event.block()).isEmpty();
    }

    @Test
    void playerInteractEventKeepsLegacyConstructorEmptyItem() {
        var event = new PlayerInteractEvent(
                proxy(Player.class),
                PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
                PlayerInteractEvent.Hand.MAIN_HAND,
                Optional.empty());

        assertThat(event.item()).isEqualTo(ItemStack.EMPTY);
    }

    @Test
    void inventoryClickEventCarriesSlotContext() {
        var inventory = proxy(Inventory.class);
        var current = new ItemStack(new TestItemType(Key.key("minecraft:stone"), 64), 3);
        var cursor = new ItemStack(new TestItemType(Key.key("minecraft:diamond"), 64), 1);

        var event = new InventoryClickEvent(
                proxy(Player.class),
                inventory,
                14,
                14,
                -1,
                5,
                ClickType.SWAP,
                2,
                current,
                cursor);

        assertThat(event.inventory()).isSameAs(inventory);
        assertThat(event.slot()).isEqualTo(14);
        assertThat(event.rawSlot()).isEqualTo(14);
        assertThat(event.containerSlot()).isEqualTo(-1);
        assertThat(event.playerInventorySlot()).isEqualTo(5);
        assertThat(event.playerInventoryClick()).isTrue();
        assertThat(event.containerClick()).isFalse();
        assertThat(event.outsideClick()).isFalse();
        assertThat(event.hotbarButton()).isEqualTo(2);
        assertThat(event.action()).isEqualTo(InventoryAction.UNKNOWN);
    }

    @Test
    void inventoryClickEventCarriesAction() {
        var event = new InventoryClickEvent(
                proxy(Player.class),
                proxy(Inventory.class),
                14,
                14,
                -1,
                5,
                ClickType.PICKUP,
                InventoryAction.PLACE_ALL,
                0,
                ItemStack.EMPTY,
                stack("minecraft:stone", 4));

        assertThat(event.action()).isEqualTo(InventoryAction.PLACE_ALL);
    }

    @Test
    void inventoryClickEventKeepsLegacyConstructorDefaults() {
        var event = new InventoryClickEvent(
                proxy(Player.class),
                proxy(Inventory.class),
                InventoryClickEvent.OUTSIDE,
                ClickType.DROP,
                0,
                ItemStack.EMPTY,
                ItemStack.EMPTY);

        assertThat(event.rawSlot()).isEqualTo(InventoryClickEvent.OUTSIDE);
        assertThat(event.containerSlot()).isEqualTo(-1);
        assertThat(event.playerInventorySlot()).isEqualTo(-1);
        assertThat(event.outsideClick()).isTrue();
        assertThat(event.hotbarButton()).isEqualTo(-1);
    }

    @Test
    void entityDamageEventCarriesSourceEntities() {
        var victim = proxy(LivingEntity.class);
        var direct = proxy(LivingEntity.class);
        var attacker = proxy(LivingEntity.class);

        var event = new EntityDamageEvent(victim, "minecraft:player_attack", 4.0, Optional.of(direct), Optional.of(attacker));

        assertThat(event.entity()).isSameAs(victim);
        assertThat(event.directEntity()).contains(direct);
        assertThat(event.attacker()).contains(attacker);
        assertThat(event.amount()).isEqualTo(4.0);
    }

    @Test
    void entityDamageEventKeepsLegacyConstructorEmptySources() {
        var event = new EntityDamageEvent(proxy(LivingEntity.class), "minecraft:fall", 2.0);

        assertThat(event.directEntity()).isEmpty();
        assertThat(event.attacker()).isEmpty();
    }

    @Test
    void commandExecuteEventNormalisesAndMutatesCommand() {
        var sender = proxy(io.fand.api.command.CommandSender.class);
        var event = new CommandExecuteEvent(sender, " /say hello");

        assertThat(event.sender()).isSameAs(sender);
        assertThat(event.originalCommand()).isEqualTo("say hello");
        event.setCommand("/fanddemo");
        event.setCancelled(true);

        assertThat(event.command()).isEqualTo("fanddemo");
        assertThat(event.cancelled()).isTrue();
    }

    @Test
    void playerCommandPreprocessEventNormalisesAndMutatesCommand() {
        var player = proxy(Player.class);
        var event = new PlayerCommandPreprocessEvent(player, "/fwhere");

        assertThat(event.player()).isSameAs(player);
        assertThat(event.originalCommand()).isEqualTo("fwhere");
        event.setCommand("fanddemo");

        assertThat(event.command()).isEqualTo("fanddemo");
    }

    @Test
    void playerDropAndPickupEventsCarryMutableItems() {
        var player = proxy(Player.class);
        var stone = stack("minecraft:stone", 3);
        var diamond = stack("minecraft:diamond", 1);

        var drop = new PlayerDropItemEvent(player, stone, true, false);
        drop.setItem(diamond);
        drop.setCancelled(true);
        var pickup = new PlayerPickupItemEvent(player, stone);
        pickup.setItem(diamond);

        assertThat(drop.player()).isSameAs(player);
        assertThat(drop.randomMotion()).isTrue();
        assertThat(drop.thrownFromHand()).isFalse();
        assertThat(drop.item()).isSameAs(diamond);
        assertThat(drop.cancelled()).isTrue();
        assertThat(pickup.item()).isSameAs(diamond);
    }

    @Test
    void playerTeleportEventCarriesMutableDestinationAndCause() {
        var player = proxy(Player.class);
        var from = location("minecraft:overworld", 0, 64, 0);
        var to = location("minecraft:overworld", 10, 70, 10);
        var retargeted = location("minecraft:the_nether", 2, 80, 2);

        var event = new PlayerTeleportEvent(player, from, to, PlayerTeleportEvent.Cause.COMMAND);
        event.setTo(retargeted);
        event.setCancelled(true);

        assertThat(event.from()).isSameAs(from);
        assertThat(event.to()).isSameAs(retargeted);
        assertThat(event.cause()).isEqualTo(PlayerTeleportEvent.Cause.COMMAND);
        assertThat(event.cancelled()).isTrue();
    }

    @Test
    void playerRespawnEventCarriesMutableLocation() {
        var player = proxy(Player.class);
        var original = location("minecraft:overworld", 0, 64, 0);
        var retargeted = location("minecraft:overworld", 5, 80, 5);

        var event = new PlayerRespawnEvent(player, original, PlayerRespawnEvent.Cause.DEATH, false);
        event.setRespawnLocation(retargeted);

        assertThat(event.player()).isSameAs(player);
        assertThat(event.respawnLocation()).isSameAs(retargeted);
        assertThat(event.cause()).isEqualTo(PlayerRespawnEvent.Cause.DEATH);
        assertThat(event.keepAllPlayerData()).isFalse();
    }

    @Test
    void entityDeathEventCarriesFatalSourceEntities() {
        var victim = proxy(LivingEntity.class);
        var attacker = proxy(LivingEntity.class);

        var event = new EntityDeathEvent(victim, "minecraft:mob_attack", Optional.empty(), Optional.of(attacker));

        assertThat(event.entity()).isSameAs(victim);
        assertThat(event.cause()).isEqualTo("minecraft:mob_attack");
        assertThat(event.directEntity()).isEmpty();
        assertThat(event.attacker()).contains(attacker);
    }

    @Test
    void entityLifecycleEventsCarryTypedCausesAndMutableTeleport() {
        var entity = proxy(Entity.class);
        var from = location("minecraft:overworld", 0, 64, 0);
        var to = location("minecraft:overworld", 10, 64, 0);
        var retargeted = location("minecraft:the_end", 0, 80, 0);

        var spawn = new EntitySpawnEvent(entity, EntitySpawnEvent.Cause.SPAWNER);
        spawn.setCancelled(true);
        var remove = new EntityRemoveEvent(entity, EntityRemoveEvent.Cause.UNLOADED_TO_CHUNK);
        var teleport = new EntityTeleportEvent(entity, from, to, EntityTeleportEvent.Cause.DIMENSION_CHANGE);
        teleport.setTo(retargeted);

        assertThat(spawn.entity()).isSameAs(entity);
        assertThat(spawn.cause()).isEqualTo(EntitySpawnEvent.Cause.SPAWNER);
        assertThat(spawn.cancelled()).isTrue();
        assertThat(remove.cause()).isEqualTo(EntityRemoveEvent.Cause.UNLOADED_TO_CHUNK);
        assertThat(teleport.to()).isSameAs(retargeted);
    }

    @Test
    void explosionPrimeEventCarriesSourceAndMutableShape() {
        var entity = proxy(Entity.class);
        var location = location("minecraft:overworld", 1, 2, 3);

        var event = new ExplosionPrimeEvent(location, Optional.of(entity), 4.0F, true);
        event.setRadius(-1.0F);
        event.setFire(false);
        event.setCancelled(true);

        assertThat(event.location()).isSameAs(location);
        assertThat(event.source()).contains(entity);
        assertThat(event.radius()).isZero();
        assertThat(event.fire()).isFalse();
        assertThat(event.cancelled()).isTrue();
    }

    @Test
    void playerItemAndStateEventsCarryMutableFields() {
        var player = proxy(Player.class);
        var stone = stack("minecraft:stone", 1);
        var diamond = stack("minecraft:diamond", 1);

        var consume = new PlayerItemConsumeEvent(player, stone);
        consume.setCancelled(true);
        var damage = new PlayerItemDamageEvent(player, stone, 3);
        damage.setDamage(5);
        var swap = new PlayerSwapHandItemsEvent(player, stone, diamond);
        swap.setMainHandItem(diamond);
        swap.setOffHandItem(stone);
        var sneak = new PlayerToggleSneakEvent(player, true);
        var sprint = new PlayerToggleSprintEvent(player, false);

        assertThat(consume.cancelled()).isTrue();
        assertThat(damage.damage()).isEqualTo(5);
        assertThat(swap.mainHandItem()).isSameAs(diamond);
        assertThat(swap.offHandItem()).isSameAs(stone);
        assertThat(sneak.sneaking()).isTrue();
        assertThat(sprint.sprinting()).isFalse();
    }

    @Test
    void playerGameModeAndKickEventsAreMutableAndCancellable() {
        var player = proxy(Player.class);
        var kick = new PlayerKickEvent(player, net.kyori.adventure.text.Component.text("bye"));
        kick.setReason(net.kyori.adventure.text.Component.text("later"));
        kick.setCancelled(true);
        var gameMode = new PlayerGameModeChangeEvent(player, GameMode.SURVIVAL, GameMode.CREATIVE);
        gameMode.setToGameMode(GameMode.ADVENTURE);

        assertThat(kick.reason()).isEqualTo(net.kyori.adventure.text.Component.text("later"));
        assertThat(kick.cancelled()).isTrue();
        assertThat(gameMode.fromGameMode()).isEqualTo(GameMode.SURVIVAL);
        assertThat(gameMode.toGameMode()).isEqualTo(GameMode.ADVENTURE);
    }

    @Test
    void blockLowLevelEventsCarryBlockTypes() {
        var block = proxy(Block.class);
        var oldType = proxy(BlockType.class);
        var newType = proxy(BlockType.class);

        var change = new BlockChangeEvent(block, oldType, newType, 3);
        change.setCancelled(true);
        var physics = new BlockPhysicsEvent(block, oldType);

        assertThat(change.block()).isSameAs(block);
        assertThat(change.oldType()).isSameAs(oldType);
        assertThat(change.newType()).isSameAs(newType);
        assertThat(change.updateFlags()).isEqualTo(3);
        assertThat(change.cancelled()).isTrue();
        assertThat(physics.sourceType()).isSameAs(oldType);
    }

    @Test
    void worldLifecycleEventsCarryWorld() {
        var world = new TestWorld(Key.key("minecraft:overworld"));

        assertThat(new WorldLoadEvent(world).world()).isSameAs(world);
        assertThat(new WorldUnloadEvent(world).world()).isSameAs(world);
        assertThat(new WorldSaveEvent(world).world()).isSameAs(world);
        assertThat(new ChunkLoadEvent(world, 1, -2).chunkX()).isEqualTo(1);
        assertThat(new ChunkUnloadEvent(world, 1, -2).chunkZ()).isEqualTo(-2);
        var weather = new WeatherChangeEvent(world, false, true);
        var thunder = new ThunderChangeEvent(world, false, true);
        thunder.setCancelled(true);
        assertThat(weather.toStorm()).isTrue();
        assertThat(thunder.toThundering()).isTrue();
        assertThat(thunder.cancelled()).isTrue();
    }

    private static <T> T proxy(Class<T> type) {
        Object instance = Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + " proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> args != null && args.length == 1 && proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
        return type.cast(instance);
    }

    private static ItemStack stack(String key, int amount) {
        return new ItemStack(new TestItemType(Key.key(key), 64), amount);
    }

    private static Location location(String key, double x, double y, double z) {
        return new Location(new TestWorld(Key.key(key)), x, y, z, 0.0F, 0.0F);
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private record TestWorld(Key key) implements World {
        @Override
        public long seed() {
            return 0;
        }

        @Override
        public long gameTime() {
            return 0;
        }

        @Override
        public CompletableFuture<Void> setGameTime(long ticks) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public long time() {
            return 0;
        }

        @Override
        public CompletableFuture<Void> setTime(long ticks) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Difficulty difficulty() {
            return Difficulty.NORMAL;
        }

        @Override
        public CompletableFuture<Void> setDifficulty(Difficulty difficulty) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean storm() {
            return false;
        }

        @Override
        public CompletableFuture<Void> setStorm(boolean storm) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean thundering() {
            return false;
        }

        @Override
        public CompletableFuture<Void> setThundering(boolean thundering) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public WorldBorder worldBorder() {
            return TestWorldBorder.INSTANCE;
        }

        @Override
        public CompletableFuture<Boolean> save() {
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public java.util.Collection<? extends Player> players() {
            return java.util.List.of();
        }

        @Override
        public void playSound(io.fand.api.world.Location location, io.fand.api.world.sound.SoundEffect sound) {
        }

        @Override
        public void spawnParticle(
                io.fand.api.world.Location location,
                io.fand.api.world.particle.ParticleEffect effect,
                io.fand.api.world.particle.ParticleEmission emission) {
        }

        @Override
        public Iterable<? extends net.kyori.adventure.audience.Audience> audiences() {
            return java.util.List.of();
        }

        @Override
        public io.fand.api.block.Block blockAt(int x, int y, int z) {
            throw new UnsupportedOperationException();
        }
    }

    private enum TestWorldBorder implements WorldBorder {
        INSTANCE;

        @Override
        public double centerX() {
            return 0;
        }

        @Override
        public double centerZ() {
            return 0;
        }

        @Override
        public void setCenter(double x, double z) {
        }

        @Override
        public double size() {
            return 0;
        }

        @Override
        public double targetSize() {
            return 0;
        }

        @Override
        public long remainingTransitionTicks() {
            return 0;
        }

        @Override
        public void setSize(double size) {
        }

        @Override
        public void setSize(double size, Duration transition) {
        }

        @Override
        public int warningDistance() {
            return 0;
        }

        @Override
        public void setWarningDistance(int blocks) {
        }

        @Override
        public int warningTime() {
            return 0;
        }

        @Override
        public void setWarningTime(int seconds) {
        }

        @Override
        public double damageBuffer() {
            return 0;
        }

        @Override
        public void setDamageBuffer(double blocks) {
        }

        @Override
        public double damageAmount() {
            return 0;
        }

        @Override
        public void setDamageAmount(double damagePerBlock) {
        }

        @Override
        public boolean contains(double x, double z) {
            return true;
        }
    }
}
