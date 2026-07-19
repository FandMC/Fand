package io.fand.server.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class EventHookSourceTest {

    private static final Set<String> PROVIDER_FIRED_EVENTS = Set.of(
            "PlayerDisguiseStateChangeEvent",
            "PlayerVanishStateChangeEvent");

    @Test
    void damageAttackerUsesGenericWrapperThenChecksLivingType() throws IOException {
        var source = read("src/main/java/io/fand/server/event/EntityEvents.java");

        assertThat(source).contains("var attacker = wrapLivingDamageSource(source.getEntity(), \"EntityDamageEvent attacker\")");
        assertThat(source).contains("var wrapped = FandHooks.wrapEntity(entity);");
        assertThat(source).contains("if (wrapped instanceof io.fand.api.entity.LivingEntity living)");
    }

    @Test
    void damageEventUsesRuntimeBreakdownAndFinalDamagePath() throws IOException {
        var hooks = read("src/main/java/io/fand/server/event/EntityEvents.java");
        var living = read("src/minecraft/java/net/minecraft/world/entity/LivingEntity.java");

        assertThat(hooks).contains("public record DamageBreakdown");
        assertThat(hooks).contains("public record DamageResult");
        assertThat(hooks).contains("damage.modifiers()");
        assertThat(living).contains("this.fand$calculateDamageBreakdown(level, source, damage)");
        assertThat(living).contains("io.fand.server.event.EntityEvents.fireDamage(this, source, fandAppliedBreakdown)");
        assertThat(living).contains("fandDamageApplicationCancelled = fandDamage.damageApplicationCancelled()");
        assertThat(living).contains("this.fand$applyFinalDamage(level, source, fandHealthDamage, fandAppliedBreakdown, fandDamageApplicationCancelled)");
        assertThat(living).contains("if (damageApplicationCancelled) {\n            return;");
    }

    @Test
    void detailedDamageEventsStayWired() throws IOException {
        var source = read("src/main/java/io/fand/server/event/EntityEvents.java");

        assertThat(source).contains("hasDetailedDamageListeners(bus)");
        assertThat(source).contains("&& !hasDetailedListeners) {\n            return DamageResult.apply(damage.finalDamage());");
        assertThat(source).contains("DamageEventRegistry.detailedEventTypes()");
        assertThat(source).contains("DamageEventRegistry.createDetailed(");

        var generator = read("../fand-data-generator/src/main/java/io/fand/datagenerator/DamageEventSpec.java");
        assertThat(generator).contains("EntityPlayerAttackDamageEvent");
        assertThat(generator).contains("EntityMobAttackDamageEvent");
        assertThat(generator).contains("EntityProjectileDamageEvent");
        assertThat(generator).contains("EntityArrowDamageEvent");
        assertThat(generator).contains("EntityTridentDamageEvent");
        assertThat(generator).contains("EntityMobProjectileDamageEvent");
        assertThat(generator).contains("EntitySpitDamageEvent");
        assertThat(generator).contains("EntityFireballDamageEvent");
        assertThat(generator).contains("EntityUnattributedFireballDamageEvent");
        assertThat(generator).contains("EntityWitherSkullDamageEvent");
        assertThat(generator).contains("EntityThrownDamageEvent");
        assertThat(generator).contains("EntityFireworksDamageEvent");
        assertThat(generator).contains("EntityWindChargeDamageEvent");
        assertThat(generator).contains("EntityExplosionDamageEvent");
        assertThat(generator).contains("EntityBadRespawnPointDamageEvent");
        assertThat(generator).contains("EntityFallDamageEvent");
        assertThat(generator).contains("EntityEnderPearlDamageEvent");
        assertThat(generator).contains("EntityFlyIntoWallDamageEvent");
        assertThat(generator).contains("EntityStalagmiteDamageEvent");
        assertThat(generator).contains("EntityFireDamageEvent");
        assertThat(generator).contains("EntityCampfireDamageEvent");
        assertThat(generator).contains("EntityHotFloorDamageEvent");
        assertThat(generator).contains("EntitySulfurCubeHotDamageEvent");
        assertThat(generator).contains("EntityLightningDamageEvent");
        assertThat(generator).contains("EntityLavaDamageEvent");
        assertThat(generator).contains("EntityDrownDamageEvent");
        assertThat(generator).contains("EntityStarveDamageEvent");
        assertThat(generator).contains("EntityFreezeDamageEvent");
        assertThat(generator).contains("EntityMagicDamageEvent");
        assertThat(generator).contains("EntityWitherDamageEvent");
        assertThat(generator).contains("EntityDragonBreathDamageEvent");
        assertThat(generator).contains("EntityThornsDamageEvent");
        assertThat(generator).contains("EntityCactusDamageEvent");
        assertThat(generator).contains("EntityOutOfWorldDamageEvent");
        assertThat(generator).contains("EntitySonicBoomDamageEvent");
        assertThat(generator).contains("EntityOutsideBorderDamageEvent");
        assertThat(generator).contains("EntityCrammingDamageEvent");
        assertThat(generator).contains("EntityInWallDamageEvent");
        assertThat(generator).contains("EntityDryOutDamageEvent");
        assertThat(generator).contains("EntitySweetBerryBushDamageEvent");
        assertThat(generator).contains("EntityFallingBlockDamageEvent");
        assertThat(generator).contains("EntityFallingAnvilDamageEvent");
        assertThat(generator).contains("EntityFallingStalactiteDamageEvent");
        assertThat(generator).contains("EntityStingDamageEvent");
        assertThat(generator).contains("EntityMaceSmashDamageEvent");
    }

    @Test
    void detailedPlayerInteractAndTeleportEventsStayWired() throws IOException {
        var playerEvents = read("src/main/java/io/fand/server/event/PlayerEvents.java");
        var packetListener = read("src/minecraft/java/net/minecraft/server/network/ServerGamePacketListenerImpl.java");

        assertThat(packetListener).contains("PlayerEvents.fireRightClickBlock(this.player, level, pos, hand, itemStack)");
        assertThat(packetListener).contains("PlayerEvents.fireRightClickAir(this.player, hand, itemStack)");
        assertThat(playerEvents).contains("PlayerMainHandRightClickBlockEvent.class");
        assertThat(playerEvents).contains("new PlayerOffHandRightClickBlockEvent(player, block, item)");
        assertThat(playerEvents).contains("new PlayerMainHandRightClickAirEvent(player, item)");
        assertThat(playerEvents).contains("new PlayerOffHandRightClickAirEvent(player, item)");
        assertThat(playerEvents).contains("bus.hasListeners(teleportEventType(cause))");
        assertThat(playerEvents).contains("new PlayerCommandTeleportEvent(player, from, to)");
        assertThat(playerEvents).contains("new PlayerPluginTeleportEvent(player, from, to)");
        assertThat(playerEvents).contains("new PlayerEnderPearlTeleportEvent(player, from, to)");
        assertThat(playerEvents).contains("new PlayerPortalTeleportEvent(player, from, to)");
        assertThat(playerEvents).contains("new PlayerSpectateTeleportEvent(player, from, to)");
        assertThat(playerEvents).contains("new PlayerUnknownTeleportEvent(player, from, to)");
    }

    @Test
    void requestedEventGapHooksStayWired() throws IOException {
        var playerEvents = read("src/main/java/io/fand/server/event/PlayerEvents.java");
        var entityEvents = read("src/main/java/io/fand/server/event/EntityEvents.java");
        var commandEvents = read("src/main/java/io/fand/server/command/CommandEvents.java");
        var pluginRuntime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var packetListener = read("src/minecraft/java/net/minecraft/server/network/ServerGamePacketListenerImpl.java");
        var itemStack = read("src/minecraft/java/net/minecraft/world/item/ItemStack.java");
        var experienceOrb = read("src/minecraft/java/net/minecraft/world/entity/ExperienceOrb.java");
        var tridentItem = read("src/minecraft/java/net/minecraft/world/item/TridentItem.java");
        var livingEntity = read("src/minecraft/java/net/minecraft/world/entity/LivingEntity.java");
        var areaEffectCloud = read("src/minecraft/java/net/minecraft/world/entity/AreaEffectCloud.java");
        var dedicatedServer = read("src/minecraft/java/net/minecraft/server/dedicated/DedicatedServer.java");

        assertThat(playerEvents).contains("new PlayerToggleFlightEvent(fandPlayer, flying)");
        assertThat(packetListener).contains("PlayerEvents.fireToggleFlight(this.player, flying)");
        assertThat(playerEvents).contains("new PlayerAnimationEvent(");
        assertThat(packetListener).contains("PlayerEvents.fireAnimation(this.player, packet.getHand())");
        assertThat(playerEvents).contains("new PlayerItemBreakEvent(fandPlayer, FandItemStacks.fromVanilla(itemStack))");
        assertThat(itemStack).contains("PlayerEvents.fireItemBreak(player, this)");
        assertThat(playerEvents).contains("new PlayerItemMendEvent(fandPlayer, FandItemStacks.fromVanilla(itemStack), repairAmount)");
        assertThat(experienceOrb).contains("PlayerEvents.fireItemMend(player, itemStack, repair)");
        assertThat(playerEvents).contains("new PlayerRiptideEvent(fandPlayer, FandItemStacks.fromVanilla(itemStack))");
        assertThat(tridentItem).contains("PlayerEvents.fireRiptide(player, itemStack)");

        assertThat(entityEvents).contains("new EntityKnockbackEvent(");
        assertThat(livingEntity).contains("EntityEvents.fireKnockback(this, source, knockbackMovement)");
        assertThat(entityEvents).contains("new AreaEffectCloudApplyEvent(apiCloud, apiAffected)");
        assertThat(areaEffectCloud).contains("EntityEvents.fireAreaEffectCloudApply(this, affectedEntities)");

        assertThat(commandEvents).contains("new ServerCommandEvent(sender(source), command)");
        assertThat(dedicatedServer).contains("CommandEvents.fireServerCommand(input.source, input.msg)");
        assertThat(dedicatedServer).contains("CommandEvents.fireServerCommand(source, command)");
        assertThat(pluginRuntime).contains("eventBus.fire(new PluginEnableEvent(descriptor))");
        assertThat(pluginRuntime).contains("eventBus.fire(new PluginDisableEvent(descriptor))");
    }

    @Test
    void requestedEventCoverageMatrixHasApiDispatcherHooksAndCancellationSemantics() throws IOException {
        var probes = List.of(
                new EventProbe(
                        "PlayerToggleFlightEvent",
                        "../fand-api/src/main/java/io/fand/api/event/player/PlayerToggleFlightEvent.java",
                        "src/main/java/io/fand/server/event/PlayerEvents.java",
                        List.of("public static boolean fireToggleFlight", "new PlayerToggleFlightEvent(fandPlayer, flying)", "return !event.cancelled();"),
                        "src/minecraft/java/net/minecraft/server/network/ServerGamePacketListenerImpl.java",
                        List.of("PlayerEvents.fireToggleFlight(this.player, flying)", "this.player.onUpdateAbilities();", "return;"),
                        List.of("implements Event, Cancellable")),
                new EventProbe(
                        "PlayerAnimationEvent",
                        "../fand-api/src/main/java/io/fand/api/event/player/PlayerAnimationEvent.java",
                        "src/main/java/io/fand/server/event/PlayerEvents.java",
                        List.of("public static boolean fireAnimation", "new PlayerAnimationEvent(", "return !event.cancelled();"),
                        "src/minecraft/java/net/minecraft/server/network/ServerGamePacketListenerImpl.java",
                        List.of("PlayerEvents.fireAnimation(this.player, packet.getHand())", "return;", "this.player.swing(packet.getHand());"),
                        List.of("implements Event, Cancellable")),
                new EventProbe(
                        "PlayerItemBreakEvent",
                        "../fand-api/src/main/java/io/fand/api/event/player/PlayerItemBreakEvent.java",
                        "src/main/java/io/fand/server/event/PlayerEvents.java",
                        List.of("public static void fireItemBreak", "new PlayerItemBreakEvent(fandPlayer, FandItemStacks.fromVanilla(itemStack))", "bus.fire("),
                        "src/minecraft/java/net/minecraft/world/item/ItemStack.java",
                        List.of("PlayerEvents.fireItemBreak(player, this)", "this.shrink(1);"),
                        List.of("implements Event")),
                new EventProbe(
                        "PlayerItemMendEvent",
                        "../fand-api/src/main/java/io/fand/api/event/player/PlayerItemMendEvent.java",
                        "src/main/java/io/fand/server/event/PlayerEvents.java",
                        List.of("public static int fireItemMend", "new PlayerItemMendEvent(fandPlayer, FandItemStacks.fromVanilla(itemStack), repairAmount)", "event.cancelled() ? 0"),
                        "src/minecraft/java/net/minecraft/world/entity/ExperienceOrb.java",
                        List.of("repair = io.fand.server.event.PlayerEvents.fireItemMend(player, itemStack, repair)", "itemStack.setDamageValue(itemStack.getDamageValue() - repair)"),
                        List.of("implements Event, Cancellable", "void setRepairAmount")),
                new EventProbe(
                        "EntityKnockbackEvent",
                        "../fand-api/src/main/java/io/fand/api/event/entity/EntityKnockbackEvent.java",
                        "src/main/java/io/fand/server/event/EntityEvents.java",
                        List.of("public static @Nullable Vec3 fireKnockback", "new EntityKnockbackEvent(", "event.cancelled() ? null"),
                        "src/minecraft/java/net/minecraft/world/entity/LivingEntity.java",
                        List.of("EntityEvents.fireKnockback(this, source, knockbackMovement)", "if (knockbackMovement == null)", "return;"),
                        List.of("implements Event, Cancellable", "void setVelocity")),
                new EventProbe(
                        "PlayerRiptideEvent",
                        "../fand-api/src/main/java/io/fand/api/event/player/PlayerRiptideEvent.java",
                        "src/main/java/io/fand/server/event/PlayerEvents.java",
                        List.of("public static void fireRiptide", "new PlayerRiptideEvent(fandPlayer, FandItemStacks.fromVanilla(itemStack))", "bus.fire("),
                        "src/minecraft/java/net/minecraft/world/item/TridentItem.java",
                        List.of("PlayerEvents.fireRiptide(player, itemStack)"),
                        List.of("implements Event")),
                new EventProbe(
                        "AreaEffectCloudApplyEvent",
                        "../fand-api/src/main/java/io/fand/api/event/entity/AreaEffectCloudApplyEvent.java",
                        "src/main/java/io/fand/server/event/EntityEvents.java",
                        List.of("public static @Nullable List<net.minecraft.world.entity.LivingEntity> fireAreaEffectCloudApply", "new AreaEffectCloudApplyEvent(apiCloud, apiAffected)", "event.cancelled() ? null"),
                        "src/minecraft/java/net/minecraft/world/entity/AreaEffectCloud.java",
                        List.of("EntityEvents.fireAreaEffectCloudApply(this, affectedEntities)", "if (affectedEntities == null)", "affectedEntities = List.of();"),
                        List.of("implements Event, Cancellable", "List<LivingEntity> affectedEntities()")),
                new EventProbe(
                        "ServerCommandEvent",
                        "../fand-api/src/main/java/io/fand/api/event/server/ServerCommandEvent.java",
                        "src/main/java/io/fand/server/command/CommandEvents.java",
                        List.of("public static Optional<String> fireServerCommand", "new ServerCommandEvent(sender(source), command)", "event.cancelled() ? Optional.empty()"),
                        "src/minecraft/java/net/minecraft/server/dedicated/DedicatedServer.java",
                        List.of("CommandEvents.fireServerCommand(input.source, input.msg)", "CommandEvents.fireServerCommand(source, command)", "performPrefixedCommand"),
                        List.of("implements Event, Cancellable", "void setCommandLine")),
                new EventProbe(
                        "PluginEnableEvent",
                        "../fand-api/src/main/java/io/fand/api/lifecycle/PluginEnableEvent.java",
                        "src/main/java/io/fand/server/plugin/PluginRuntime.java",
                        List.of("private void firePluginEnableEvent", "eventBus.fire(new PluginEnableEvent(descriptor))", "loadedPlugin.plugin.onEnable(loadedPlugin.context)"),
                        "src/main/java/io/fand/server/plugin/PluginRuntime.java",
                        List.of("loadedPlugin.enabled = true;", "firePluginEnableEvent(loadedPlugin.descriptor)"),
                        List.of("implements Event")),
                new EventProbe(
                        "PluginDisableEvent",
                        "../fand-api/src/main/java/io/fand/api/lifecycle/PluginDisableEvent.java",
                        "src/main/java/io/fand/server/plugin/PluginRuntime.java",
                        List.of("private void firePluginDisableEvent", "eventBus.fire(new PluginDisableEvent(descriptor))", "LOGGER.warn(\"PluginDisableEvent listener failed"),
                        "src/main/java/io/fand/server/plugin/PluginRuntime.java",
                        List.of("if (wasEnabled) {", "firePluginDisableEvent(loadedPlugin.descriptor)", "loadedPlugin.enabled = false;"),
                        List.of("implements Event")));

        for (var probe : probes) {
            assertProbe(probe);
        }
    }

    @Test
    void apiEventsAreNotOrphanedFromServerOrMinecraftSources() throws IOException {
        var sourceIndex = readAllSources(
                Path.of("src/main/java"),
                Path.of("src/minecraft/java"));
        var orphaned = new ArrayList<String>();

        for (var event : apiEventClasses()) {
            if (!sourceIndex.contains(event) && !PROVIDER_FIRED_EVENTS.contains(event)) {
                orphaned.add(event);
            }
        }

        assertThat(orphaned)
                .as("API events must be server-wired or explicitly designated as provider-fired")
                .isEmpty();
    }

    @Test
    void villagerReputationEventStaysWired() throws IOException {
        var entityEvents = read("src/main/java/io/fand/server/event/EntityEvents.java");
        var serverLevel = read("src/minecraft/java/net/minecraft/server/level/ServerLevel.java");
        var living = read("src/minecraft/java/net/minecraft/world/entity/LivingEntity.java");

        assertThat(entityEvents).contains("public static boolean fireDamageReaction");
        assertThat(entityEvents).contains("new EntityDamageReactionEvent(");
        assertThat(entityEvents).contains("EntityDamageReactionEvent.Cause.ENTITY_TARGET");
        assertThat(entityEvents).contains("EntityDamageReactionEvent.Cause.VILLAGER_REPUTATION");
        assertThat(entityEvents).contains("targetImpact(target, cause)");
        assertThat(entityEvents).contains("public static boolean fireVillagerReputation");
        assertThat(entityEvents).contains("new VillagerReputationEvent(apiVillager, fandSource, reputationCause(type))");
        assertThat(serverLevel).contains("if (!io.fand.server.event.EntityEvents.fireVillagerReputation(type, source, target))");
        assertThat(living).contains("EntityDamageReactionEvent.Cause.HURT_MEMORY");
        assertThat(living).contains("EntityDamageReactionEvent.Cause.HURT_BY_PLAYER");
        assertThat(living).contains("EntityDamageReactionEvent.Cause.HURT_BY_MOB");
    }

    @Test
    void offlinePlayerDataCanBeMutatedAndSaved() throws IOException {
        var offline = read("src/main/java/io/fand/server/player/FandOfflinePlayer.java");
        var playerList = read("src/minecraft/java/net/minecraft/server/players/PlayerList.java");
        var storage = read("src/minecraft/java/net/minecraft/world/level/storage/PlayerDataStorage.java");

        assertThat(offline).contains("public CompletableFuture<Boolean> save()");
        assertThat(offline).contains("handle.setItem(slot, FandItemStacks.toVanilla(stack))");
        assertThat(offline).contains("writeInventory(data, inventory, server)");
        assertThat(playerList).contains("public boolean savePlayerData");
        assertThat(storage).contains("public boolean save(");
    }

    @Test
    void projectileEventsPreserveShooterPayload() throws IOException {
        var source = read("src/main/java/io/fand/server/event/EntityEvents.java");

        assertThat(source).contains("projectile.getOwner() == null ? null : FandHooks.wrapEntity(projectile.getOwner())");
        assertThat(source).contains("potion.getOwner() == null ? null : FandHooks.wrapEntity(potion.getOwner())");
    }

    @Test
    void entitySpawnDropCancellationAndRemovePayloadPathsStayWired() throws IOException {
        var source = read("src/main/java/io/fand/server/event/EntityEvents.java");

        assertThat(source).contains("public static boolean fireSpawn");
        assertThat(source).contains("if (event.cancelled()) {\n                return false;");
        assertThat(source).contains("if (event.cancelled() || event.item().empty()) {\n                return false;");
        assertThat(source).contains("public static net.minecraft.world.item.@Nullable ItemStack fireDropItem");
        assertThat(source).contains("return null;");
        assertThat(source).contains("public static void fireRemove");
        assertThat(source).contains("new EntityRemoveEvent(fandEntity, removeCause(entity.getRemovalReason()))");
    }

    @Test
    void blockCancellationHasRollbackHooksForGeneratedChanges() throws IOException {
        var source = read("src/main/java/io/fand/server/event/BlockEvents.java");

        assertThat(source).contains("restoreStructureGrow(level, snapshot)");
        assertThat(source).contains("level.setBlock(pos, entry.getValue(), Block.UPDATE_ALL)");
        assertThat(source).contains("return !event.cancelled();");
    }

    @Test
    void blockItemPlaceCancellationRestoresNeighbourChangesAndComponents() throws IOException {
        var source = read("src/minecraft/java/net/minecraft/world/item/BlockItem.java");

        assertThat(source).contains("snapshotPlacementStates(level, pos)");
        assertThat(source).contains("snapshotPlacementComponents(serverLevel, beforeStates.keySet())");
        assertThat(source).contains("changedPlacementPositions(level, beforeStates)");
        assertThat(source).contains("restorePlacement(serverLevel, beforeStates, beforeComponents, serverPlayer)");
        assertThat(source).contains("level.getBlockTicks().clearArea(restoreArea(beforeStates))");
        assertThat(source).contains("level.setBlock(pos, entry.getValue(), Block.UPDATE_ALL)");
    }

    @Test
    void customBlockExplosionDropsRemovalAndResistanceStayWired() throws IOException {
        var registry = read("src/main/java/io/fand/server/block/FandCustomBlockRegistry.java");
        var hooks = read("src/main/java/io/fand/server/hooks/FandHooks.java");
        var level = read("src/minecraft/java/net/minecraft/world/level/Level.java");
        var blockBehaviour = read("src/minecraft/java/net/minecraft/world/level/block/state/BlockBehaviour.java");
        var explosionCalculator = read("src/minecraft/java/net/minecraft/world/level/ExplosionDamageCalculator.java");

        assertThat(registry).contains("public @Nullable List<net.minecraft.world.item.ItemStack> explosionDrops(");
        assertThat(registry).contains("explosionDecayAmount(drop.getCount(), explosion.radius(), level.getRandom()::nextFloat)");
        assertThat(registry).doesNotContain("events.subscribe(BlockBreakEvent.class");
        assertThat(hooks).contains("public static void customBlockRemoved(ServerLevel level");
        assertThat(level).containsSubsequence(
                "FandHooks.customBlockRemoved(serverLevel, pos)",
                "BlockComponentStorage.clearIfBlockChanged(serverLevel, pos, oldState, newState)");
        assertThat(level).contains("FandHooks.customBlockDrops(serverLevel, pos)");
        assertThat(blockBehaviour).contains("FandHooks.customBlockExplosionDrops(level, pos, explosion)");
        assertThat(blockBehaviour).contains("if (fandDrops != null)");
        assertThat(explosionCalculator).contains("FandHooks.customBlockBlastResistance(");
    }

    @Test
    void tntPathCancellationIsCoveredByBlockItemPlacementRollbackWindow() throws IOException {
        var source = read("src/minecraft/java/net/minecraft/world/item/BlockItem.java");

        assertThat(source).contains("for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 2, 1)))");
        assertThat(source).contains("BlockState replacedState = level.getBlockState(pos)");
        assertThat(source).contains("placedState.getBlock().setPlacedBy(level, pos, placedState, player, itemStack)");
        assertThat(source).contains("if (!allowed) {");
        assertThat(source).contains("return InteractionResult.FAIL;");
    }

    @Test
    void playerSpeedCheckAndCommandLoggingAreConfigGated() throws IOException {
        var source = read("src/minecraft/java/net/minecraft/server/network/ServerGamePacketListenerImpl.java");

        assertThat(source).contains("FandHooks.playerSpeedCheckEnabled() && movedDist - expectedDist > 100.0");
        assertThat(source).contains("if (!io.fand.server.hooks.FandHooks.playerSpeedCheckEnabled())");
        assertThat(source).contains("FandHooks.playerCommandLoggingEnabled()");
        assertThat(source).contains("issued server command: /{}");
    }

    @Test
    void customLootAdvancementAndEnchantmentRuntimeHooksStayWired() throws IOException {
        var lootTable = read("src/minecraft/java/net/minecraft/world/level/storage/loot/LootTable.java");
        var reloadableRegistries = read("src/minecraft/java/net/minecraft/server/ReloadableServerRegistries.java");
        var advancementManager = read("src/minecraft/java/net/minecraft/server/ServerAdvancementManager.java");
        var mappedRegistry = read("src/minecraft/java/net/minecraft/core/MappedRegistry.java");
        var runtime = read("src/main/java/io/fand/server/FandServer.java");
        var enchantments = read("src/main/java/io/fand/server/enchantment/FandEnchantmentRegistry.java");

        assertThat(lootTable).contains("FandHooks.generateLootReplacement(this.fand$key, params)");
        assertThat(reloadableRegistries).contains(".fand$key(id)");
        assertThat(advancementManager).contains("fand$addCustomAdvancement");
        assertThat(advancementManager).contains("fand$removeCustomAdvancement");
        assertThat(mappedRegistry).contains("fand$registerRuntime");
        assertThat(runtime).contains("advancements.applyLoadedAdvancements()");
        assertThat(runtime).contains("enchantments.applyLoadedEnchantments()");
        assertThat(enchantments).contains("mapped.fand$registerRuntime");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static void assertProbe(EventProbe probe) throws IOException {
        var api = read(probe.apiPath());
        assertThat(api).as(probe.name() + " API file").contains(probe.apiMarkers().toArray(String[]::new));

        var dispatcher = read(probe.dispatcherPath());
        assertThat(dispatcher).as(probe.name() + " dispatcher").contains(probe.dispatcherMarkers().toArray(String[]::new));

        var hook = read(probe.hookPath());
        assertThat(hook).as(probe.name() + " hook").contains(probe.hookMarkers().toArray(String[]::new));
    }

    private static List<String> apiEventClasses() throws IOException {
        var roots = List.of(
                Path.of("../fand-api/src/main/java/io/fand/api/event"),
                Path.of("../fand-api/src/main/java/io/fand/api/lifecycle"));
        var events = new ArrayList<String>();
        for (var root : roots) {
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith("Event.java"))
                        .filter(path -> !path.getFileName().toString().equals("Event.java"))
                        .map(path -> path.getFileName().toString().replace(".java", ""))
                        .forEach(events::add);
            }
        }
        events.sort(String::compareTo);
        return events;
    }

    private static String readAllSources(Path... roots) throws IOException {
        var builder = new StringBuilder();
        for (var root : roots) {
            try (var stream = Files.walk(root)) {
                var files = stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .sorted()
                        .toList();
                for (var file : files) {
                    builder.append(read(file.toString())).append('\n');
                }
            }
        }
        return builder.toString();
    }

    private record EventProbe(
            String name,
            String apiPath,
            String dispatcherPath,
            List<String> dispatcherMarkers,
            String hookPath,
            List<String> hookMarkers,
            List<String> apiMarkers
    ) {
    }
}
