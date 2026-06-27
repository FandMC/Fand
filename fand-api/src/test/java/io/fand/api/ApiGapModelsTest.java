package io.fand.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.command.CommandArgument;
import io.fand.api.command.CommandArgumentType;
import io.fand.api.event.entity.DamageCause;
import io.fand.api.event.entity.DamageModifier;
import io.fand.api.event.entity.EntityDamageEvent;
import io.fand.api.block.FluidState;
import io.fand.api.block.FluidTypes;
import io.fand.api.entity.Entity;
import io.fand.api.entity.GameMode;
import io.fand.api.entity.Player;
import io.fand.api.integration.ExternalIntegration;
import io.fand.api.integration.ExternalIntegrationKind;
import io.fand.api.integration.ExternalIntegrationStrategy;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.persistence.PersistentDataContainer;
import io.fand.api.player.PlayerProfile;
import io.fand.api.tablist.RemoteTabListEntry;
import io.fand.api.tablist.TabListEntry;
import io.fand.api.tablist.TabListGroup;
import io.fand.api.tablist.TabListLayout;
import io.fand.api.tablist.TabListRegistration;
import io.fand.api.tablist.TabListService;
import io.fand.api.tablist.TabListSyncStrategy;
import io.fand.api.world.Vector3;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class ApiGapModelsTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);

    @Test
    void persistentDataRoundTripsThroughItemCustomData() {
        var key = Key.key("example", "owner");
        var stack = new ItemStack(DIAMOND, 1)
                .withPersistentData(PersistentDataContainer.EMPTY.withString(key, "alice"));

        assertThat(stack.persistentData().getString(key)).contains("alice");

        var cleared = stack.withoutPersistentData(key);
        assertThat(cleared.persistentData().empty()).isTrue();
        assertThat(cleared.customData()).isEmpty();
    }

    @Test
    void commandArgumentDescribesTypedSelectors() {
        var argument = CommandArgument.players("targets").asOptional();

        assertThat(argument.name()).isEqualTo("targets");
        assertThat(argument.type()).isEqualTo(CommandArgumentType.PLAYERS);
        assertThat(argument.optional()).isTrue();
    }

    @Test
    void damageEventKeepsLegacyCauseAndTypedCause() {
        var event = new EntityDamageEvent(new TestLivingEntity(), DamageCause.FALL, 7.0);

        assertThat(event.cause()).isEqualTo("minecraft:fall");
        assertThat(event.causeKey()).isEqualTo(Key.key("minecraft:fall"));

        event.setModifier(DamageModifier.ARMOR, -2.0);
        assertThat(event.amount()).isEqualTo(5.0);
        assertThat(event.modifiers()).containsEntry(DamageModifier.BASE, 7.0);

        event.setAmount(3.0);
        assertThat(event.amount()).isEqualTo(3.0);
        assertThat(event.modifiers()).containsOnly(Map.entry(DamageModifier.BASE, 3.0));
    }

    @Test
    void damageEventAcceptsRuntimeDamageBreakdown() {
        var event = new EntityDamageEvent(
                new TestLivingEntity(),
                DamageCause.PLAYER_ATTACK,
                4.0,
                Map.of(
                        DamageModifier.BASE, 8.0,
                        DamageModifier.ARMOR, -2.0,
                        DamageModifier.RESISTANCE, -1.0,
                        DamageModifier.ABSORPTION, -1.0),
                null,
                null);

        assertThat(event.amount()).isEqualTo(4.0);
        assertThat(event.modifier(DamageModifier.BASE)).isEqualTo(8.0);
        assertThat(event.modifier(DamageModifier.ARMOR)).isEqualTo(-2.0);
        assertThat(event.modifier(DamageModifier.RESISTANCE)).isEqualTo(-1.0);
        assertThat(event.modifier(DamageModifier.ABSORPTION)).isEqualTo(-1.0);
    }

    @Test
    void fluidStateExposesFriendlyPredicates() {
        var water = new FluidState(
                FluidTypes.WATER,
                true,
                true,
                false,
                8,
                1.0F,
                1.0F,
                100.0F,
                Vector3.ZERO);
        var lava = new FluidState(
                FluidTypes.FLOWING_LAVA,
                false,
                false,
                true,
                4,
                0.5F,
                0.5F,
                100.0F,
                new Vector3(1.0, 0.0, 0.0));

        assertThat(water.water()).isTrue();
        assertThat(water.source()).isTrue();
        assertThat(water.flowing()).isFalse();
        assertThat(lava.lava()).isTrue();
        assertThat(lava.flowing()).isTrue();
        assertThat(lava.falling()).isTrue();
        assertThat(FluidState.none().empty()).isTrue();
    }

    @Test
    void tabListGroupAndLayoutBuildViewerRows() {
        var alice = player("Alice", 5, GameMode.SURVIVAL);
        var bob = player("Bob", 25, GameMode.CREATIVE);
        var group = TabListGroup.of(player -> player.ping() >= 10)
                .withOrder(Comparator.comparing(Player::name).reversed())
                .withOrderBase(40);

        var layout = TabListLayout.from(group, List.of(alice, bob));
        var service = new RecordingTabListService();
        layout.apply(service, alice);

        assertThat(layout.entries()).hasSize(1);
        assertThat(layout.entries().getFirst().profile().name()).isEqualTo("Bob");
        assertThat(layout.entries().getFirst().latency()).isEqualTo(25);
        assertThat(layout.entries().getFirst().gameMode()).isEqualTo(GameMode.CREATIVE);
        assertThat(layout.entries().getFirst().order()).isEqualTo(40);
        assertThat(service.entriesByViewer.get(alice.uniqueId())).hasSize(1);
    }

    @Test
    void tabListSyncStrategyAppliesRemoteEntries() {
        var viewer = player("Viewer", 0, GameMode.SURVIVAL);
        var remote = TabListEntry.builder(UUID.randomUUID(), "Remote").latency(80).build();
        TabListSyncStrategy strategy = ignored -> List.of(new RemoteTabListEntry(Key.key("proxy:lobby"), remote));
        var service = new RecordingTabListService();

        service.sync(viewer, strategy);

        assertThat(service.entriesByViewer.get(viewer.uniqueId())).containsKey(remote.profile().uniqueId());
    }

    @Test
    void externalIntegrationStrategyFindsDeclaredServices() {
        var redis = new ExternalIntegration(
                Key.key("fand:redis"),
                ExternalIntegrationKind.REDIS,
                Map.of("host", "127.0.0.1"));
        ExternalIntegrationStrategy strategy = () -> List.of(redis);

        assertThat(strategy.integration(Key.key("fand:redis"))).contains(redis);
        assertThat(strategy.integration(Key.key("fand:mysql"))).isEmpty();
        assertThat(ExternalIntegrationStrategy.empty().integrations()).isEmpty();
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private static Player player(String name, int ping, GameMode gameMode) {
        return player(name, ping, gameMode, (proxy, method, args) -> null);
    }

    private static Player player(String name, int ping, GameMode gameMode, InvocationHandler custom) {
        var uniqueId = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        InvocationHandler handler = (proxy, method, args) -> {
            var customResult = custom.invoke(proxy, method, args);
            if (customResult != null) {
                return customResult;
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> name;
                    case "hashCode" -> uniqueId.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                };
            }
            return switch (method.getName()) {
                case "uniqueId" -> uniqueId;
                case "entityId" -> 1;
                case "name" -> name;
                case "profile" -> new PlayerProfile(uniqueId, name);
                case "ping" -> ping;
                case "gameMode" -> gameMode;
                case "tabListDisplayName" -> Optional.empty();
                case "sendMessage", "setVelocity", "setCustomName", "setCustomNameVisible",
                        "setGlowing", "setSilent", "setGravity", "setInvulnerable",
                        "addScoreboardTag", "removeScoreboardTag", "remove", "ejectPassengers",
                        "kick", "playSound", "sendTabList", "clearTabList",
                        "setTabListDisplayName", "setTabListOrder", "sendResourcePack",
                        "removeResourcePack", "setGameMode", "setFoodLevel", "setSaturation",
                        "setExperienceLevel", "setExperienceProgress", "giveExperience",
                        "setFlying", "setAllowFlight", "discoverRecipes", "undiscoverRecipes",
                        "setCooldown", "clearCooldown", "setStatistic", "setRespawnLocation",
                        "sendCompassTarget", "setCursorItem", "closeInventory" -> null;
                default -> defaultValue(method.getReturnType());
            };
        };
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0F;
        }
        if (returnType == double.class) {
            return 0.0D;
        }
        if (returnType == Optional.class) {
            return Optional.empty();
        }
        if (returnType == Collection.class || returnType == List.class) {
            return List.of();
        }
        return null;
    }

    private static final class RecordingTabListService implements TabListService {

        private final Map<UUID, Map<UUID, TabListEntry>> entriesByViewer = new LinkedHashMap<>();

        @Override
        public Collection<? extends TabListRegistration> entries(Player viewer) {
            return List.of();
        }

        @Override
        public Optional<? extends TabListRegistration> entry(Player viewer, UUID entryId) {
            return Optional.empty();
        }

        @Override
        public TabListRegistration add(Player viewer, TabListEntry entry) {
            entriesByViewer.computeIfAbsent(viewer.uniqueId(), ignored -> new LinkedHashMap<>())
                    .put(entry.profile().uniqueId(), entry);
            return new TabListRegistration() {
                @Override
                public UUID viewerId() {
                    return viewer.uniqueId();
                }

                @Override
                public UUID entryId() {
                    return entry.profile().uniqueId();
                }

                @Override
                public void update(TabListEntry replacement) {
                    entriesByViewer.get(viewer.uniqueId()).put(replacement.profile().uniqueId(), replacement);
                }

                @Override
                public void remove() {
                    entriesByViewer.getOrDefault(viewer.uniqueId(), Map.of()).remove(entry.profile().uniqueId());
                }

                @Override
                public boolean active() {
                    return true;
                }
            };
        }

        @Override
        public boolean remove(Player viewer, UUID entryId) {
            return entriesByViewer.getOrDefault(viewer.uniqueId(), Map.of()).remove(entryId) != null;
        }

        @Override
        public void removeAll(Player viewer) {
            entriesByViewer.remove(viewer.uniqueId());
        }
    }

    private static final class TestLivingEntity implements io.fand.api.entity.LivingEntity {
        @Override
        public java.util.UUID uniqueId() {
            return new java.util.UUID(0L, 1L);
        }

        @Override
        public int entityId() {
            return 1;
        }

        @Override
        public io.fand.api.entity.EntityType type() {
            return new io.fand.api.entity.EntityType() {
                @Override
                public Key key() {
                    return Key.key("minecraft:pig");
                }

                @Override
                public boolean spawnable() {
                    return true;
                }

                @Override
                public boolean player() {
                    return false;
                }
            };
        }

        @Override
        public boolean alive() {
            return true;
        }

        @Override
        public io.fand.api.world.Location location() {
            return null;
        }

        @Override
        public io.fand.api.world.World world() {
            return null;
        }

        @Override
        public io.fand.api.world.Vector3 velocity() {
            return new io.fand.api.world.Vector3(0, 0, 0);
        }

        @Override
        public void setVelocity(io.fand.api.world.Vector3 velocity) {
        }

        @Override
        public java.util.Optional<net.kyori.adventure.text.Component> customName() {
            return java.util.Optional.empty();
        }

        @Override
        public void setCustomName(net.kyori.adventure.text.Component name) {
        }

        @Override
        public boolean customNameVisible() {
            return false;
        }

        @Override
        public void setCustomNameVisible(boolean visible) {
        }

        @Override
        public boolean glowing() {
            return false;
        }

        @Override
        public void setGlowing(boolean glowing) {
        }

        @Override
        public boolean silent() {
            return false;
        }

        @Override
        public void setSilent(boolean silent) {
        }

        @Override
        public boolean gravity() {
            return true;
        }

        @Override
        public void setGravity(boolean gravity) {
        }

        @Override
        public boolean invulnerable() {
            return false;
        }

        @Override
        public void setInvulnerable(boolean invulnerable) {
        }

        @Override
        public java.util.Set<String> scoreboardTags() {
            return java.util.Set.of();
        }

        @Override
        public void addScoreboardTag(String tag) {
        }

        @Override
        public void removeScoreboardTag(String tag) {
        }

        @Override
        public double width() {
            return 1;
        }

        @Override
        public double height() {
            return 1;
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> teleport(io.fand.api.world.Location destination) {
            return java.util.concurrent.CompletableFuture.completedFuture(true);
        }

        @Override
        public void remove() {
        }

        @Override
        public java.util.Optional<? extends io.fand.api.entity.Entity> vehicle() {
            return java.util.Optional.empty();
        }

        @Override
        public List<? extends io.fand.api.entity.Entity> passengers() {
            return List.of();
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> mount(io.fand.api.entity.Entity vehicle) {
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> addPassenger(io.fand.api.entity.Entity passenger) {
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> removePassenger(io.fand.api.entity.Entity passenger) {
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> dismount() {
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }

        @Override
        public void ejectPassengers() {
        }

        @Override
        public boolean onGround() {
            return true;
        }

        @Override
        public boolean inWater() {
            return false;
        }

        @Override
        public boolean inLava() {
            return false;
        }

        @Override
        public int fireTicks() {
            return 0;
        }

        @Override
        public void setFireTicks(int ticks) {
        }

        @Override
        public int ticksLived() {
            return 0;
        }

        @Override
        public io.fand.api.component.DataComponentContainer components() {
            return null;
        }

        @Override
        public double health() {
            return 20;
        }

        @Override
        public double maxHealth() {
            return 20;
        }

        @Override
        public void setHealth(double health) {
        }

        @Override
        public boolean dead() {
            return false;
        }

        @Override
        public void damage(double amount) {
        }

        @Override
        public void damage(double amount, io.fand.api.entity.Entity source) {
        }

        @Override
        public void heal(double amount) {
        }

        @Override
        public double absorption() {
            return 0;
        }

        @Override
        public void setAbsorption(double absorption) {
        }

        @Override
        public int armor() {
            return 0;
        }

        @Override
        public java.util.Optional<? extends io.fand.api.entity.Attribute> attribute(Key key) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Collection<io.fand.api.entity.EntityEffect> effects() {
            return List.of();
        }

        @Override
        public java.util.Optional<io.fand.api.entity.EntityEffect> effect(Key key) {
            return java.util.Optional.empty();
        }

        @Override
        public void addEffect(io.fand.api.entity.EntityEffect effect) {
        }

        @Override
        public void removeEffect(Key key) {
        }

        @Override
        public ItemStack equipment(io.fand.api.item.component.ItemEquipmentSlot slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setEquipment(io.fand.api.item.component.ItemEquipmentSlot slot, ItemStack item) {
        }
    }
}
