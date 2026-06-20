package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ApiSurfaceSourceTest {

    @Test
    void playerHighFrequencyApiIsBackedByServerImplementation() throws IOException {
        var player = read("src/main/java/io/fand/server/entity/FandPlayer.java");
        var visibility = read("src/main/java/io/fand/server/entity/EntityVisibility.java");
        var registry = read("src/main/java/io/fand/server/entity/PlayerRegistry.java");

        assertThat(player).contains(
                "public PlayerScoreboard scoreboard()",
                "public void setSkin(@Nullable PlayerSkin skin)",
                "public void refreshSkin()",
                "public Location eyeLocation()",
                "public float flySpeed()",
                "public void setFlySpeed(float speed)",
                "public float walkSpeed()",
                "public void setWalkSpeed(float speed)",
                "public void sendBlockChange(Location location, io.fand.api.block.BlockType type)",
                "new ClientboundBlockUpdatePacket",
                "public void openBook(ItemStack book)",
                "openItemGui",
                "public void openSign(Location location)",
                "openTextEdit",
                "public void hideEntity(Player viewer, io.fand.api.entity.Entity entity)",
                "public void showEntity(Player viewer, io.fand.api.entity.Entity entity)",
                "public void respawn()",
                "server.getPlayerList().respawn(handle, false, RemovalReason.KILLED)",
                "public boolean hasCooldown(Key group)",
                "public void setCooldown(Key group, int ticks)",
                "getCooldowns().addCooldown(identifier, ticks)");
        assertThat(visibility).contains(
                "ChunkMap.class",
                "\"TrackedEntity\"",
                "\"removePlayer\"",
                "\"updatePlayer\"");
        assertThat(registry).contains("new FandPlayer(handle, permissions, this, scoreboards, tabLists)");
    }

    @Test
    void livingEntityStateAndSleepApiIsBackedByServerImplementation() throws IOException {
        var api = read("../fand-api/src/main/java/io/fand/api/entity/LivingEntity.java");
        var living = read("src/main/java/io/fand/server/entity/FandLivingEntity.java");
        var player = read("src/main/java/io/fand/server/entity/FandPlayer.java");

        assertThat(api).contains(
                "default Optional<? extends LivingEntity> target()",
                "default void setTarget(@Nullable LivingEntity target)",
                "default boolean ai()",
                "default void setAi(boolean ai)",
                "default boolean noAi()",
                "default void setNoAi(boolean noAi)",
                "default boolean aggressive()",
                "default void setAggressive(boolean aggressive)",
                "default boolean persistent()",
                "default void setPersistent()");
        assertThat(living).contains(
                "public Optional<? extends LivingEntity> target()",
                "public void setTarget(@Nullable LivingEntity target)",
                "public boolean noAi()",
                "public void setNoAi(boolean noAi)",
                "public boolean aggressive()",
                "public void setAggressive(boolean aggressive)",
                "public boolean persistent()",
                "public void setPersistent()",
                "private net.minecraft.world.entity.Mob requireMob",
                "public int remainingAir()",
                "public void setRemainingAir(int ticks)",
                "public int freezeTicks()",
                "public void setFreezeTicks(int ticks)",
                "public int invulnerableTicks()",
                "public void setInvulnerableTicks(int ticks)",
                "public boolean lineOfSight",
                "public boolean sleeping()",
                "public Optional<Location> sleepingLocation()",
                "public boolean sleep(Location location)",
                "handle().startSleeping(pos)",
                "public void wakeUp()");
        assertThat(player).contains(
                "public int remainingAir()",
                "public int freezeTicks()",
                "public int invulnerableTicks()",
                "public boolean lineOfSight",
                "handle.startSleepInBed(pos)",
                "handle.stopSleepInBed(true, true)");
    }

    @Test
    void worldControlApiDoesNotFallBackToDefaultUnsupportedPaths() throws IOException {
        var world = read("src/main/java/io/fand/server/world/FandWorld.java");

        assertThat(world).contains(
                "public Location spawnLocation()",
                "public CompletableFuture<Void> setSpawnLocation(Location location)",
                "public CompletableFuture<Optional<? extends Entity>> spawnEntity(",
                "EntitySpawnOptionsApplier.apply(entity, options)",
                "handle.addFreshEntity(entity)",
                "public CompletableFuture<Optional<? extends ItemEntity>> dropItem(",
                "new net.minecraft.world.entity.item.ItemEntity",
                "public CompletableFuture<Optional<? extends Entity>> strikeLightning(Location location, boolean visualOnly)",
                "EntityTypes.LIGHTNING_BOLT.create",
                "public CompletableFuture<Void> createExplosion(Location location, float power, boolean fire, boolean breakBlocks)",
                "handle.explode(",
                "public CompletableFuture<Boolean> loadChunk(int chunkX, int chunkZ)",
                "handle.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true)",
                "public CompletableFuture<Boolean> setChunkForceLoaded(int chunkX, int chunkZ, boolean forceLoaded)",
                "handle.setChunkForced(chunkX, chunkZ, forceLoaded)",
                "public CompletableFuture<BlockBatchResult> setBlocks(",
                "new BlockBatchRunner(requested, changes, options, future)",
                "private PlayerRegistry fallbackPlayerRegistry()",
                "new FandScoreboardService(handle.getServer())");
    }

    @Test
    void scoreboardTeamAndPerPlayerScoreboardsStayImplemented() throws IOException {
        var apiTeam = read("../fand-api/src/main/java/io/fand/api/scoreboard/ScoreboardTeam.java");
        var apiPlayerScoreboard = read("../fand-api/src/main/java/io/fand/api/scoreboard/PlayerScoreboard.java");
        var service = read("src/main/java/io/fand/server/scoreboard/FandScoreboardService.java");
        var playerScoreboard = read("src/main/java/io/fand/server/scoreboard/FandPlayerScoreboard.java");
        var team = read("src/main/java/io/fand/server/scoreboard/FandPlayerScoreboardTeam.java");

        assertThat(apiTeam).contains(
                "TeamVisibility nameTagVisibility()",
                "void setNameTagVisibility(TeamVisibility visibility)",
                "TeamCollisionRule collisionRule()",
                "boolean addPlayer(Player player)",
                "boolean addEntity(Entity entity)");
        assertThat(apiPlayerScoreboard).contains(
                "ScoreboardRegistration registerTeam(String name)",
                "void setDisplayedObjective(ScoreDisplaySlot slot, ScoreboardObjective objective)",
                "void resetDisplayedObjectives()");
        assertThat(service).contains("registerPlayerScoreboard", "unregisterPlayerScoreboard", "resendPlayerOverrides");
        assertThat(playerScoreboard).contains(
                "new Scoreboard()",
                "ClientboundSetDisplayObjectivePacket",
                "ClientboundSetPlayerTeamPacket",
                "resendDisplayedObjectives");
        assertThat(team).contains(
                "setNameTagVisibility",
                "setCollisionRule",
                "scoreboard.sendTeamPlayer(handle, owner, ClientboundSetPlayerTeamPacket.Action.ADD)");
    }

    @Test
    void permissionTreeAndPluginNamespaceRegistrationStayImplemented() throws IOException {
        var manager = read("src/main/java/io/fand/server/permission/PermissionManager.java");
        var pluginService = read("src/main/java/io/fand/server/plugin/PluginPermissionService.java");
        var runtime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var descriptor = read("../fand-api/src/main/java/io/fand/api/permission/PermissionDescriptor.java");

        assertThat(descriptor).contains("Map<String, Boolean> children");
        assertThat(manager).contains(
                "public void recalculate(PermissionSubject subject)",
                "public void recalculateAll()",
                "descriptorChildValue",
                "descriptor.children().containsKey(normalized)",
                "Permission already registered with different children");
        assertThat(pluginService).contains(
                "validatePluginPermissionNode(pluginId, descriptor.node())",
                "validatePluginPermissionNode(pluginId, child)",
                "delegate.register(descriptor)",
                "delegate.recalculate(subject)",
                "delegate.recalculateAll()");
        assertThat(runtime).contains(
                "validateDescriptorPermission",
                "registerDeclaredPermissions",
                "permissions.register(permission)",
                "permissionNamespaces(pluginId)");
    }

    @Test
    void pluginScopedLootTableApiStaysWiredToRuntimeAndLifecycleCleanup() throws IOException {
        var context = read("../fand-api/src/main/java/io/fand/api/plugin/PluginContext.java");
        var runtimeContext = read("src/main/java/io/fand/server/plugin/RuntimePluginContext.java");
        var runtime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var pluginLoot = read("src/main/java/io/fand/server/plugin/PluginLootTableService.java");
        var tracker = read("src/main/java/io/fand/server/plugin/PluginResourceTracker.java");
        var server = read("src/main/java/io/fand/server/FandServer.java");

        assertThat(context).contains("default LootTableService lootTables()");
        assertThat(runtimeContext).contains(
                "private final LootTableService lootTables",
                "public LootTableService lootTables()");
        assertThat(runtime).contains(
                "private final LootTableService lootTableService",
                "new PluginLootTableService(lootTableService, resources, artifact.descriptor.id())");
        assertThat(pluginLoot).contains(
                "public final class PluginLootTableService implements LootTableService",
                "return delegate.table(scopedKey(key)).filter(this::ownedByThisPlugin)",
                "return tracker.track(delegate.replace(scopedKey(key), generator))");
        assertThat(tracker).contains(
                "TrackedLootTableRegistration track(LootTableRegistration delegate)",
                "lootTableRegistrationsToClose",
                "registration.unregisterFromTracker()");
        assertThat(server).contains(
                "this.lootTables = new FandLootTableService(minecraftServer::get)",
                "lootTables,",
                "public LootTableService lootTables()");
    }

    @Test
    void pluginMapRenderersStayScopedToPluginLifecycle() throws IOException {
        var runtime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var pluginMap = read("src/main/java/io/fand/server/plugin/PluginMapService.java");
        var tracker = read("src/main/java/io/fand/server/plugin/PluginResourceTracker.java");
        var mapService = read("src/main/java/io/fand/server/map/FandMapService.java");

        assertThat(runtime).contains("new PluginMapService(mapService, resources)");
        assertThat(pluginMap).contains(
                "public final class PluginMapService implements MapService",
                "return delegate.map(id).map(this::wrap)",
                "tracker.track(new PluginResourceTracker.MapRendererBinding(delegate, id(), renderer))");
        assertThat(tracker).contains(
                "record MapRendererBinding(MapService service, int mapId, MapRenderer renderer)",
                "mapRendererBindingsToClose",
                "maps.clearRenderer(mapId, renderer)");
        assertThat(mapService).contains(
                "public void clearRenderer(int id, MapRenderer renderer)",
                "state.renderer() == renderer ? null : state");
    }

    @Test
    void pluginStructureTemplatesStayScopedToPluginNamespace() throws IOException {
        var runtime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var pluginStructures = read("src/main/java/io/fand/server/plugin/PluginStructureService.java");
        var serverStructures = read("src/main/java/io/fand/server/structure/FandStructureService.java");

        assertThat(runtime).contains("new PluginStructureService(structureService, artifact.descriptor.id())");
        assertThat(pluginStructures).contains(
                "public final class PluginStructureService implements StructureService",
                "return delegate.template(scopedKey(key)).filter(this::ownedByThisPlugin)",
                "return delegate.save(scopedKey(key), volume)",
                "exportTemplate(Key key, StructureFormat format)",
                "return delegate.exportTemplate(scopedKey(key), format)",
                "importTemplate(Key key, StructureProjection projection)",
                "return delegate.importTemplate(scopedKey(key), projection)",
                "save(Key key, Path path, StructureFormat format)",
                "return delegate.place(scopedKey(key), origin, placement)",
                "return delegate.locate(structure, origin, radius)");
        assertThat(serverStructures).contains(
                "template(Key key)",
                "save(Key key, StructureVolume volume)",
                "exportTemplate(Key key, StructureFormat format)",
                "importTemplate(Key key, StructureProjection projection)",
                "load(Path path, StructureFormat format)",
                "save(Key key, Path path, StructureFormat format)",
                "StructureFormat.SPONGE_SCHEMATIC",
                "StructureFormat.WORLDEDIT_SCHEMATIC",
                "spongeToVanilla",
                "legacyWorldEditToVanilla",
                "decodeVarInts",
                "BlockRotProcessor",
                "BlockIgnoreProcessor.STRUCTURE_BLOCK",
                "place(Key key, Location origin, StructurePlacement placement)",
                "locate(Key structure, Location origin, int radius)");
    }

    @Test
    void miniMessageParserStaysExposedAndBackedByPlaceholders() throws IOException {
        var server = read("src/main/java/io/fand/server/FandServer.java");
        var context = read("../fand-api/src/main/java/io/fand/api/plugin/PluginContext.java");
        var runtimeContext = read("src/main/java/io/fand/server/plugin/RuntimePluginContext.java");
        var runtime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var service = read("src/main/java/io/fand/server/text/FandMiniMessageService.java");

        assertThat(context).contains(
                "default MiniMessageService miniMessages()",
                "return MiniMessageService.empty()");
        assertThat(server).contains(
                "this.miniMessages = new FandMiniMessageService(placeholders)",
                "public MiniMessageService miniMessages()");
        assertThat(runtimeContext).contains(
                "private final MiniMessageService miniMessages",
                "public MiniMessageService miniMessages()");
        assertThat(runtime).contains(
                "var pluginPlaceholders = new PluginPlaceholderService",
                "new FandMiniMessageService(pluginPlaceholders)");
        assertThat(service).contains(
                "MiniMessage.miniMessage()",
                "placeholders.replace(viewer, input)",
                "parser.deserialize");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
