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
                "public void hideEntity(io.fand.api.entity.Entity entity)",
                "public void showEntity(io.fand.api.entity.Entity entity)",
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
        var block = read("src/main/java/io/fand/server/block/FandBlock.java");
        var apiWorld = read("../fand-api/src/main/java/io/fand/api/world/World.java");

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
                "public CompletableFuture<BlockScanResult> replaceFluids(",
                "new FluidScanRunner(clamped, matcher, replacement, options, future)",
                "public CompletableFuture<BlockScanResult> clearFluids(",
                "new FluidScanRunner(clamped, matcher, null, options, future)",
                "public CompletableFuture<BlockScanResult> replaceConnectedFluids(",
                "new ConnectedFluidReplaceRunner(",
                "public CompletableFuture<BlockScanResult> clearConnectedFluids(",
                "clearFluidAt(pos, options.batchOptions())",
                "BlockComponentStorage.empty(handle, pos)",
                "private PlayerRegistry fallbackPlayerRegistry()",
                "new FandScoreboardService(handle.getServer())");
        assertThat(block).contains(
                "level.getFluidIfLoaded(pos)",
                "public boolean water()",
                "public boolean lava()");
        assertThat(apiWorld).contains(
                "default CompletableFuture<BlockScanResult> replaceFluids(",
                "default CompletableFuture<BlockScanResult> clearFluids(",
                "default CompletableFuture<BlockScanResult> replaceConnectedFluids(",
                "default CompletableFuture<BlockScanResult> clearConnectedFluids(");
    }

    @Test
    void aiSensorLoopFastPathsStayWiredToVanillaSensors() throws IOException {
        var playerSensor = read("src/minecraft/java/net/minecraft/world/entity/ai/sensing/PlayerSensor.java");
        var temptingSensor = read("src/minecraft/java/net/minecraft/world/entity/ai/sensing/TemptingSensor.java");
        var nearestItemSensor = read("src/minecraft/java/net/minecraft/world/entity/ai/sensing/NearestItemSensor.java");
        var golemSensor = read("src/minecraft/java/net/minecraft/world/entity/ai/sensing/GolemSensor.java");
        var mobSensor = read("src/minecraft/java/net/minecraft/world/entity/ai/sensing/MobSensor.java");
        var breezeSensor = read("src/minecraft/java/net/minecraft/world/entity/ai/sensing/BreezeAttackEntitySensor.java");
        var wardenSensor = read("src/minecraft/java/net/minecraft/world/entity/ai/sensing/WardenEntitySensor.java");
        var hooks = read("src/main/java/io/fand/server/hooks/FandHooks.java");
        var config = read("src/main/java/io/fand/server/config/FandConfig.java");
        var reloader = read("src/main/java/io/fand/server/config/ConfigReloader.java");

        assertThat(playerSensor).contains(
                "FandHooks.aiSensorLoopFastPathEnabled()",
                "private void doTickFast",
                "players.sort(Comparator.comparingDouble(body::distanceToSqr))");
        assertThat(temptingSensor).contains(
                "FandHooks.aiSensorLoopFastPathEnabled()",
                "private Player nearestTemptingPlayer",
                "double nearestDistance = Double.MAX_VALUE");
        assertThat(nearestItemSensor).contains(
                "FandHooks.aiSensorLoopFastPathEnabled()",
                "private Optional<ItemEntity> nearestVisibleWantedItem",
                "level.forEachEntity(");
        assertThat(golemSensor).contains(
                "FandHooks.aiSensorLoopFastPathEnabled()",
                "for (LivingEntity entity : livingEntitiesMemory.get())");
        assertThat(mobSensor).contains(
                "FandHooks.aiSensorLoopFastPathEnabled()",
                "for (LivingEntity entity : livingEntitiesMemory.get())");
        assertThat(breezeSensor).contains(
                "FandHooks.aiSensorLoopFastPathEnabled()",
                "private Optional<LivingEntity> findNearestAttackable");
        assertThat(wardenSensor).contains(
                "FandHooks.aiSensorLoopFastPathEnabled()",
                "private static Optional<LivingEntity> getClosestFast");
        assertThat(hooks).contains(
                "private static volatile boolean aiSensorLoopFastPath = true",
                "aiSensorLoopFastPath = performance.aiSensorLoopFastPath",
                "public static boolean aiSensorLoopFastPathEnabled()");
        assertThat(config).contains("public volatile boolean aiSensorLoopFastPath = true");
        assertThat(reloader).contains("performance.aiSensorLoopFastPath");
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
        var service = read("../fand-api/src/main/java/io/fand/api/permission/PermissionService.java");
        var context = read("../fand-api/src/main/java/io/fand/api/permission/PermissionContext.java");
        var meta = read("../fand-api/src/main/java/io/fand/api/permission/PermissionMeta.java");
        var group = read("../fand-api/src/main/java/io/fand/api/permission/PermissionGroup.java");
        var descriptor = read("../fand-api/src/main/java/io/fand/api/permission/PermissionDescriptor.java");

        assertThat(descriptor).contains("Map<String, Boolean> children");
        assertThat(service).contains(
                "default PermissionMeta meta(PermissionSubject subject, PermissionContext context)",
                "default Optional<String> prefix(PermissionSubject subject, PermissionContext context)",
                "default Optional<String> suffix(PermissionSubject subject, PermissionContext context)",
                "default Optional<String> metaValue(PermissionSubject subject, PermissionContext context, String key)",
                "default Optional<String> primaryGroup(PermissionSubject subject, PermissionContext context)",
                "default Collection<String> groups(PermissionSubject subject, PermissionContext context)",
                "default Optional<PermissionGroup> group(String name, PermissionContext context)",
                "default Collection<PermissionGroup> groups(PermissionContext context)");
        assertThat(context).contains(
                "public record PermissionContext",
                "Optional<String> value(String key)",
                "PermissionContext with(String key, String value)");
        assertThat(meta).contains(
                "public record PermissionMeta",
                "Optional<String> primaryGroup",
                "List<String> groups",
                "public Optional<String> value(String key)");
        assertThat(group).contains(
                "public record PermissionGroup",
                "List<String> parents",
                "PermissionMeta meta");
        assertThat(manager).contains(
                "public void recalculate(PermissionSubject subject)",
                "public void recalculateAll()",
                "public PermissionMeta meta(PermissionSubject subject, PermissionContext context)",
                "public Optional<PermissionGroup> group(String name, PermissionContext context)",
                "public Collection<PermissionGroup> groups(PermissionContext context)",
                "descriptorChildValue",
                "childParents",
                "PermissionChildParent",
                "Permission already registered with different children");
        assertThat(pluginService).contains(
                "validatePluginPermissionNode(pluginId, descriptor.node())",
                "validatePluginPermissionNode(pluginId, child)",
                "delegate.register(descriptor)",
                "delegate.meta(subject, context)",
                "delegate.group(name, context)",
                "delegate.groups(context)",
                "delegate.recalculate(subject)",
                "delegate.recalculateAll()");
        assertThat(runtime).contains(
                "validateDescriptorPermission",
                "registerDeclaredPermissions",
                "permissions.register(permission)",
                "permissionNamespaces(pluginId)");
    }

    @Test
    void regionProtectionResolutionApiStaysImplemented() throws IOException {
        var region = read("../fand-api/src/main/java/io/fand/api/region/Region.java");
        var definition = read("../fand-api/src/main/java/io/fand/api/region/RegionDefinition.java");
        var protection = read("../fand-api/src/main/java/io/fand/api/region/RegionProtection.java");
        var resolution = read("../fand-api/src/main/java/io/fand/api/region/RegionFlagResolution.java");
        var trace = read("../fand-api/src/main/java/io/fand/api/region/RegionFlagTrace.java");
        var service = read("../fand-api/src/main/java/io/fand/api/region/RegionService.java");
        var server = read("src/main/java/io/fand/server/region/FandRegionService.java");
        var plugin = read("src/main/java/io/fand/server/plugin/PluginRegionService.java");

        assertThat(region).contains(
                "RegionProtection protection",
                "public <T> Optional<T> explicitFlag(RegionFlag<T> flag)");
        assertThat(definition).contains(
                "RegionProtection protection",
                "public Builder priority(int priority)",
                "public Builder parent(Key parent)",
                "public Builder owner(String owner)",
                "public Builder member(String member)");
        assertThat(protection).contains(
                "public record RegionProtection",
                "int priority",
                "Optional<Key> parent",
                "Set<String> owners",
                "Set<String> members");
        assertThat(resolution).contains(
                "public record RegionFlagResolution",
                "List<RegionFlagTrace<T>> trace");
        assertThat(trace).contains(
                "public record RegionFlagTrace",
                "boolean inherited");
        assertThat(service).contains(
                "default <T> RegionFlagResolution<T> resolveFlag",
                "resolveFlagFromRegion",
                "region.protection().parent()");
        assertThat(server).contains(
                "entry.region().protection().priority()",
                "readProtection",
                "protectionToJson");
        assertThat(plugin).contains(
                "return applicableRegions(location).stream().findFirst()",
                "scopedProtection(region.protection())");
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
    void pluginScopedDataPackApiStaysWiredToRuntimeAndLifecycleCleanup() throws IOException {
        var serverApi = read("../fand-api/src/main/java/io/fand/api/Server.java");
        var context = read("../fand-api/src/main/java/io/fand/api/plugin/PluginContext.java");
        var runtimeContext = read("src/main/java/io/fand/server/plugin/RuntimePluginContext.java");
        var runtime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var pluginDataPacks = read("src/main/java/io/fand/server/plugin/PluginDataPackService.java");
        var tracker = read("src/main/java/io/fand/server/plugin/PluginResourceTracker.java");
        var server = read("src/main/java/io/fand/server/FandServer.java");
        var service = read("src/main/java/io/fand/server/datapack/FandDataPackService.java");

        assertThat(serverApi).contains("default DataPackService dataPacks()");
        assertThat(context).contains("default DataPackService dataPacks()");
        assertThat(runtimeContext).contains(
                "private final DataPackService dataPacks",
                "public DataPackService dataPacks()");
        assertThat(runtime).contains(
                "private final DataPackService dataPackService",
                "new PluginDataPackService(dataPackService, resources, id)");
        assertThat(pluginDataPacks).contains(
                "public final class PluginDataPackService implements DataPackService",
                "return delegate.packs().stream()",
                "return tracker.track(delegate.create(new DataPack(scopedId(pack.id()), pack.description(), pack.enabled())))",
                "Plugin data pack files must stay under data/");
        assertThat(tracker).contains(
                "TrackedDataPackRegistration track(DataPackRegistration delegate)",
                "dataPackRegistrationsToClose",
                "registration.closeFromTracker()",
                "static final class TrackedDataPackRegistration implements DataPackRegistration");
        assertThat(server).contains(
                "this.dataPacks = new FandDataPackService(Path.of(\"datapacks\"), minecraftServer::get)",
                "dataPacks,",
                "public DataPackService dataPacks()",
                "return dataPacks.reload()");
        assertThat(service).contains(
                "public final class FandDataPackService implements DataPackService",
                "private static final String WORLD_PACK_PREFIX = \"fand-\"",
                "current.getWorldPath(LevelResource.DATAPACK_DIR)",
                "return \"file/\" + worldPackDirectoryName(id)");
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
        assertThat(read("../fand-api/src/main/java/io/fand/api/map/MapView.java")).contains(
                "Plugin-scoped services remove renderers",
                "saved map data and is not automatically reverted when a plugin unloads");
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
        var source = read("src/main/java/io/fand/server/world/FandWorldGeneratorSource.java");
        var chunkMap = read("src/minecraft/java/net/minecraft/server/level/ChunkMap.java");
        var chunkCache = read("src/minecraft/java/net/minecraft/server/level/ServerChunkCache.java");
        var nmsStructure = read("src/minecraft/java/net/minecraft/world/level/levelgen/structure/structures/FandTemplateStructure.java");
        var nmsStructureTypes = read("src/minecraft/java/net/minecraft/world/level/levelgen/structure/StructureType.java");
        var nmsPieceTypes = read("src/minecraft/java/net/minecraft/world/level/levelgen/structure/pieces/StructurePieceType.java");

        assertThat(runtime).contains("new PluginStructureService(structureService, resources, artifact.descriptor.id())");
        assertThat(pluginStructures).contains(
                "public final class PluginStructureService implements StructureService",
                "public StructureRegistration registerStructure(CustomStructure structure)",
                "return tracker.track(delegate.registerStructure(new CustomStructure(",
                "public StructureRegistration registerStructureSet(CustomStructureSet structureSet)",
                "return tracker.track(delegate.registerStructureSet(new CustomStructureSet(",
                "return delegate.template(scopedKey(key)).filter(this::ownedByThisPlugin)",
                "return delegate.save(scopedKey(key), volume)",
                "exportTemplate(Key key, StructureFormat format)",
                "return delegate.exportTemplate(scopedKey(key), format)",
                "importTemplate(Key key, StructureProjection projection)",
                "return delegate.importTemplate(scopedKey(key), projection)",
                "save(Key key, Path path, StructureFormat format)",
                "return delegate.place(scopedKey(key), origin, placement)",
                "return delegate.locate(scopedKey(structure), origin, radius)");
        assertThat(serverStructures).contains(
                "registerStructure(CustomStructure structure)",
                "registerStructureSet(CustomStructureSet structureSet)",
                "applyLoadedStructures()",
                "structureSetHolders()",
                "refreshStructureStates(current)",
                "level.getChunkSource().fand$refreshGeneratorState()",
                "registerVanillaStructure(MinecraftServer server, CustomStructure structure)",
                "registerVanillaStructureSet(MinecraftServer server, CustomStructureSet structureSet)",
                "new FandTemplateStructure(",
                "template(Key key)",
                "save(Key key, StructureVolume volume)",
                "exportTemplate(Key key, StructureFormat format)",
                "importTemplate(Key key, StructureProjection projection)",
                "load(Path path, StructureFormat format)",
                "save(Key key, Path path, StructureFormat format)",
                "StructureFormat.SPONGE_SCHEMATIC",
                "StructureFormat.WORLDEDIT_SCHEMATIC",
                "StructureFormat.BLU",
                "StructureFormat.LITEMATIC",
                "bluToVanilla",
                "writeBlu",
                "litematicToVanilla",
                "writeLitematic",
                "spongeToVanilla",
                "legacyWorldEditToVanilla",
                "decodeVarInts",
                "BlockRotProcessor",
                "BlockIgnoreProcessor.STRUCTURE_BLOCK",
                "place(Key key, Location origin, StructurePlacement placement)",
                "placeEphemeral(StructureProjection projection, Location origin, StructurePlacement placement)",
                "placeEphemeralOnServerThread",
                "server.getStructureManager().remove(id)",
                "locate(Key structure, Location origin, int radius)");
        assertThat(source).contains(
                "new FilteredStructureSetLookup(structureSets, settings, structures)",
                "structures.structureSetHolders().filter(this::enabled)",
                "structures.runtimeStructureSetOwned(apiKey(holder.key()))",
                "structureSetReferencesActive(holder)",
                "structures::runtimeStructureActive");
        assertThat(chunkMap).contains(
                "private volatile ChunkGeneratorStructureState chunkGeneratorState",
                "public void fand$refreshGeneratorState()",
                "this.generator().createState(registryAccess.lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, this.level.getSeed())");
        assertThat(chunkCache).contains("public void fand$refreshGeneratorState()");
        assertThat(nmsStructure).contains(
                "public final class FandTemplateStructure extends Structure",
                "public static final MapCodec<FandTemplateStructure> CODEC",
                "return StructureType.FAND_TEMPLATE",
                "StructurePieceType.FAND_TEMPLATE",
                "setIgnoreEntities(!includeEntities)");
        assertThat(nmsStructureTypes).contains("StructureType<FandTemplateStructure> FAND_TEMPLATE = register(\"fand:template\", FandTemplateStructure.CODEC)");
        assertThat(nmsPieceTypes).contains("StructurePieceType FAND_TEMPLATE = setTemplatePieceId(FandTemplateStructure.Piece::new, \"fand:template\")");
    }

    @Test
    void customBiomeDefinitionsAreRegisteredBeforeWorldgenUsesBiomeLookup() throws IOException {
        var api = read("../fand-api/src/main/java/io/fand/api/world/generation/BiomeProvider.java");
        var registry = read("src/main/java/io/fand/server/world/FandBiomeRegistry.java");
        var server = read("src/main/java/io/fand/server/FandServer.java");
        var source = read("src/main/java/io/fand/server/world/FandBiomeSource.java");

        assertThat(api).contains("default List<CustomBiomeDefinition> customBiomes()");
        assertThat(registry).contains(
                "public final class FandBiomeRegistry",
                "settings.biomeProvider().customBiomes()",
                "ResourceKey.create(Registries.BIOME, identifier(definition.key()))",
                "new Biome.BiomeBuilder()",
                "definition.features()",
                "Registries.PLACED_FEATURE");
        assertThat(server).contains("FandBiomeRegistry.applyCustomBiomes(server, settings)");
        assertThat(source).contains(
                "for (var definition : provider.customBiomes())",
                "holders.add(resolve(definition.key()))",
                "return biomes.get(resourceKey).map(holder -> (Holder<Biome>) holder).orElse(fallback)");
    }

    @Test
    void fandDataEventsAndRuntimeRecipePredicatesStayWired() throws IOException {
        var playerApi = read("../fand-api/src/main/java/io/fand/api/entity/Player.java");
        var offlinePlayerApi = read("../fand-api/src/main/java/io/fand/api/player/OfflinePlayer.java");
        var recipeIngredient = read("../fand-api/src/main/java/io/fand/api/recipe/RecipeIngredient.java");
        var player = read("src/main/java/io/fand/server/entity/FandPlayer.java");
        var advancementStorage = read("src/main/java/io/fand/server/component/AdvancementDataStorage.java");
        var persistentData = read("src/main/java/io/fand/server/component/PersistentComponentData.java");
        var stats = read("src/minecraft/java/net/minecraft/stats/ServerStatsCounter.java");
        var playerEvents = read("src/main/java/io/fand/server/event/PlayerEvents.java");
        var recipes = read("src/main/java/io/fand/server/recipe/FandRecipes.java");
        var runtimeRecipes = read("src/main/java/io/fand/server/recipe/FandRuntimeCraftingRecipes.java");

        assertThat(playerApi).contains(
                "PersistentDataContainer advancementData(Key advancement)",
                "void setAdvancementData(Key advancement, PersistentDataContainer data)",
                "default void incrementStatistic(StatisticKey key, int delta)");
        assertThat(offlinePlayerApi).contains(
                "default int statistic(StatisticKey key)");
        assertThat(recipeIngredient).contains(
                "static RecipeIngredient matching",
                "Predicate<ItemStack>",
                "runtimeMatched()",
                "matches(ItemStack stack)");
        assertThat(player).contains(
                "public PersistentDataContainer advancementData(Key advancement)",
                "AdvancementDataStorage.get(server, uniqueId(), advancement)",
                "public void setAdvancementData(Key advancement, PersistentDataContainer data)",
                "AdvancementDataStorage.set(server, uniqueId(), advancement, data)");
        assertThat(advancementStorage).contains(
                "public final class AdvancementDataStorage",
                "PersistentComponentData.advancementType()",
                "playerId + \"/\" + Identifier.fromNamespaceAndPath");
        assertThat(persistentData).contains("SavedDataType<PersistentComponentData> advancementType()");
        assertThat(stats).contains(
                "PlayerEvents.fireStatisticIncrement(serverPlayer, stat, previousValue, count)",
                "if (fandValue == null)",
                "super.setValue(player, stat, nextValue)");
        assertThat(playerEvents).contains(
                "new PlayerStatisticIncrementEvent(fandPlayer, statisticKey(stat), previousValue, newValue)",
                "event.cancelled() ? null : event.newValue()");
        assertThat(recipes).contains(
                "FandRuntimeCraftingRecipes.hasRuntimeIngredient(recipe)",
                "FandRuntimeCraftingRecipes.shaped(",
                "FandRuntimeCraftingRecipes.shapeless(");
        assertThat(runtimeRecipes).contains(
                "final class FandRuntimeCraftingRecipes",
                "RuntimeShapedRecipe extends net.minecraft.world.item.crafting.ShapedRecipe",
                "RuntimeShapelessRecipe extends net.minecraft.world.item.crafting.ShapelessRecipe",
                "fallback.test(stack) && FandRuntimeCraftingRecipes.matches(api, stack)");
    }

    @Test
    void miniMessageParserStaysExposedAndBackedByPlaceholders() throws IOException {
        var server = read("src/main/java/io/fand/server/FandServer.java");
        var context = read("../fand-api/src/main/java/io/fand/api/plugin/PluginContext.java");
        var runtimeContext = read("src/main/java/io/fand/server/plugin/RuntimePluginContext.java");
        var runtime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var service = read("src/main/java/io/fand/server/text/FandMiniMessageService.java");
        var placeholderContext = read("../fand-api/src/main/java/io/fand/api/placeholder/PlaceholderContext.java");
        var placeholderProvider = read("../fand-api/src/main/java/io/fand/api/placeholder/PlaceholderProvider.java");
        var placeholderService = read("../fand-api/src/main/java/io/fand/api/placeholder/PlaceholderService.java");
        var serverPlaceholders = read("src/main/java/io/fand/server/placeholder/FandPlaceholderService.java");
        var pluginPlaceholders = read("src/main/java/io/fand/server/plugin/PluginPlaceholderService.java");

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
        assertThat(placeholderContext).contains(
                "public record PlaceholderContext",
                "@Nullable Player viewer",
                "@Nullable Player target",
                "@Nullable World world",
                "@Nullable Entity entity",
                "Map<String, Object> values");
        assertThat(placeholderProvider).contains(
                "static PlaceholderProvider contextual",
                "resolve(PlaceholderContext context, String identifier)");
        assertThat(placeholderService).contains(
                "default Optional<String> resolve(String identifier, PlaceholderContext context)",
                "default String replace(String input, PlaceholderContext context)");
        assertThat(serverPlaceholders).contains("provider.resolve(context, normalized)");
        assertThat(pluginPlaceholders).contains("delegate.resolve(identifier, context)");
    }

    @Test
    void tabListSyncAndPlayerInfoPacketApiStayExposed() throws IOException {
        var tabService = read("../fand-api/src/main/java/io/fand/api/tablist/TabListService.java");
        var group = read("../fand-api/src/main/java/io/fand/api/tablist/TabListGroup.java");
        var layout = read("../fand-api/src/main/java/io/fand/api/tablist/TabListLayout.java");
        var sync = read("../fand-api/src/main/java/io/fand/api/tablist/TabListSyncStrategy.java");
        var remote = read("../fand-api/src/main/java/io/fand/api/tablist/RemoteTabListEntry.java");
        var factory = read("../fand-api/src/main/java/io/fand/api/packet/PlayerInfoPacketFactory.java");
        var registryApi = read("../fand-api/src/main/java/io/fand/api/packet/PacketRegistry.java");
        var serverFactory = read("src/main/java/io/fand/server/network/packet/FandPlayerInfoPacketFactory.java");
        var registry = read("src/main/java/io/fand/server/network/packet/PacketRegistryImpl.java");
        var bridge = read("src/main/java/io/fand/server/network/packet/VanillaPacketBridge.java");
        var pluginPackets = read("src/main/java/io/fand/server/plugin/PluginPacketRegistry.java");
        var server = read("src/main/java/io/fand/server/FandServer.java");

        assertThat(tabService).contains(
                "default void apply(Player viewer, TabListLayout layout)",
                "default void sync(Player viewer, TabListSyncStrategy strategy)");
        assertThat(group).contains("public record TabListGroup", "entries(Collection<? extends Player> players)");
        assertThat(layout).contains("public record TabListLayout", "void apply(TabListService service, Player viewer)");
        assertThat(sync).contains("interface TabListSyncStrategy", "Collection<RemoteTabListEntry> entries(Player viewer)");
        assertThat(remote).contains("record RemoteTabListEntry(Key source, TabListEntry entry)");
        assertThat(factory).contains(
                "interface PlayerInfoPacketFactory",
                "PacketView add(Collection<? extends TabListEntry> entries)",
                "PacketView remove(Collection<UUID> entryIds)");
        assertThat(registryApi).contains("default PlayerInfoPacketFactory playerInfo()");
        assertThat(serverFactory).contains(
                "public final class FandPlayerInfoPacketFactory implements PlayerInfoPacketFactory",
                "PacketType.PLAY_CLIENTBOUND_PLAYER_INFO_UPDATE",
                "PacketType.PLAY_CLIENTBOUND_PLAYER_INFO_REMOVE");
        assertThat(registry).contains(
                "private final PlayerInfoPacketFactory playerInfo = new FandPlayerInfoPacketFactory()",
                "public PacketRegistryImpl(Supplier<@Nullable MinecraftServer> server)",
                "public PlayerInfoPacketFactory playerInfo()");
        assertThat(bridge).contains(
                "FandTabListPackets.packetFromApi",
                "new ClientboundPlayerInfoRemovePacket(profileIds(view))");
        assertThat(pluginPackets).contains("public PlayerInfoPacketFactory playerInfo()");
        assertThat(server).contains("this.packets = new PacketRegistryImpl(minecraftServer::get)");
    }

    @Test
    void directPacketAndViewerIllusionApiStayExposed() throws IOException {
        var packetRegistry = read("../fand-api/src/main/java/io/fand/api/packet/PacketRegistry.java");
        var sender = read("../fand-api/src/main/java/io/fand/api/packet/PacketSender.java");
        var illusions = read("../fand-api/src/main/java/io/fand/api/packet/ViewerIllusionService.java");
        var builder = read("../fand-api/src/main/java/io/fand/api/packet/PacketBuilder.java");
        var helpers = read("../fand-api/src/main/java/io/fand/api/packet/PacketHelpers.java");
        var registry = read("src/main/java/io/fand/server/network/packet/PacketRegistryImpl.java");
        var serverSender = read("src/main/java/io/fand/server/network/packet/FandPacketSender.java");
        var serverIllusions = read("src/main/java/io/fand/server/network/packet/FandViewerIllusionService.java");
        var bridge = read("src/main/java/io/fand/server/network/packet/VanillaPacketBridge.java");
        var pluginPackets = read("src/main/java/io/fand/server/plugin/PluginPacketRegistry.java");

        assertThat(packetRegistry).contains(
                "default PacketSender sender()",
                "default ViewerIllusionService illusions()",
                "default PacketHelpers helpers()",
                "default PacketBuilder builder(PacketType type)",
                "default PacketView packet(PacketType type, Map<String, ?> fields)");
        assertThat(builder).contains(
                "public final class PacketBuilder",
                "PacketView build()",
                "boolean send(Player viewer)");
        assertThat(helpers).contains(
                "public final class PacketHelpers",
                "entityMetadata",
                "hologramEntity",
                "scoreboardTeam",
                "openScreen");
        assertThat(sender).contains(
                "interface PacketSender",
                "boolean send(Player viewer, PacketView packet)");
        assertThat(illusions).contains(
                "interface ViewerIllusionService",
                "boolean fakeBlock(Player viewer, Location location, BlockType type)",
                "boolean fakeEntity(Player viewer, PacketView spawnPacket)",
                "boolean removeFakeEntity(Player viewer, int entityId)");
        assertThat(registry).contains(
                "private final FandPacketSender sender",
                "private final ViewerIllusionService illusions",
                "public PacketView packet(PacketType type, Map<String, ?> fields)");
        assertThat(serverSender).contains(
                "Only clientbound packets can be sent to players",
                "bridge.toVanilla(packet)",
                "ServerThreading.run");
        assertThat(serverIllusions).contains(
                "new ClientboundBlockUpdatePacket",
                "new ClientboundRemoveEntitiesPacket(entityId)",
                "viewer.hideEntity(entity)",
                "viewer.showEntity(entity)");
        assertThat(bridge).contains(
                "@Nullable Packet<?> toVanilla(PacketView view)",
                "adaptValue",
                "AdventureBridge.toVanilla",
                "private static Class<?> packetClass(PacketType type)");
        assertThat(pluginPackets).contains(
                "public PacketSender sender()",
                "public ViewerIllusionService illusions()",
                "public PacketView packet(PacketType type, Map<String, ?> fields)");
    }

    @Test
    void externalIntegrationStrategyStaysWiredToServerAndPlugins() throws IOException {
        var serverApi = read("../fand-api/src/main/java/io/fand/api/Server.java");
        var context = read("../fand-api/src/main/java/io/fand/api/plugin/PluginContext.java");
        var integration = read("../fand-api/src/main/java/io/fand/api/integration/ExternalIntegration.java");
        var strategy = read("../fand-api/src/main/java/io/fand/api/integration/ExternalIntegrationStrategy.java");
        var server = read("src/main/java/io/fand/server/FandServer.java");
        var runtime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var runtimeContext = read("src/main/java/io/fand/server/plugin/RuntimePluginContext.java");

        assertThat(serverApi).contains("default ExternalIntegrationStrategy integrations()");
        assertThat(context).contains("default ExternalIntegrationStrategy integrations()");
        assertThat(integration).contains("record ExternalIntegration", "ExternalIntegrationKind kind");
        assertThat(strategy).contains(
                "interface ExternalIntegrationStrategy",
                "Collection<ExternalIntegration> integrations()",
                "static ExternalIntegrationStrategy empty()");
        assertThat(server).contains(
                "private final ExternalIntegrationStrategy integrations",
                "this.integrations = ExternalIntegrationStrategy.empty()",
                "this.plugins.integrations(integrations)",
                "public ExternalIntegrationStrategy integrations()");
        assertThat(runtime).contains(
                "private volatile ExternalIntegrationStrategy integrationStrategy = ExternalIntegrationStrategy.empty()",
                "public void integrations(ExternalIntegrationStrategy strategy)",
                "integrationStrategy,");
        assertThat(runtimeContext).contains(
                "private final ExternalIntegrationStrategy integrations",
                "public ExternalIntegrationStrategy integrations()");
    }

    @Test
    void serviceRegistryStaysWiredToServerPluginsAndLifecycleCleanup() throws IOException {
        var serverApi = read("../fand-api/src/main/java/io/fand/api/Server.java");
        var managerApi = read("../fand-api/src/main/java/io/fand/api/plugin/PluginManager.java");
        var context = read("../fand-api/src/main/java/io/fand/api/plugin/PluginContext.java");
        var registryApi = read("../fand-api/src/main/java/io/fand/api/service/ServiceRegistry.java");
        var serverRegistry = read("src/main/java/io/fand/server/service/FandServiceRegistry.java");
        var pluginRegistry = read("src/main/java/io/fand/server/plugin/PluginServiceRegistry.java");
        var runtime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var runtimeContext = read("src/main/java/io/fand/server/plugin/RuntimePluginContext.java");
        var tracker = read("src/main/java/io/fand/server/plugin/PluginResourceTracker.java");
        var server = read("src/main/java/io/fand/server/FandServer.java");

        assertThat(serverApi).contains("default ServiceRegistry services()");
        assertThat(managerApi).contains("default ServiceRegistry services()");
        assertThat(context).contains("default ServiceRegistry services()");
        assertThat(registryApi).contains(
                "interface ServiceRegistry",
                "Collection<ServiceProvider<?>> providers()",
                "ServiceRegistration<T> register");
        assertThat(serverRegistry).contains(
                "public final class FandServiceRegistry implements ServiceRegistry",
                "ServiceProvider<>",
                "register(");
        assertThat(pluginRegistry).contains(
                "final class PluginServiceRegistry implements ServiceRegistry",
                "tracker.track",
                "registry.register(key, type, service, priority, owner)");
        assertThat(runtime).contains(
                "private volatile ServiceRegistry serviceRegistry = new FandServiceRegistry()",
                "public ServiceRegistry services()",
                "new PluginServiceRegistry(serviceRegistry, resources, id)");
        assertThat(runtimeContext).contains(
                "private final ServiceRegistry services",
                "public ServiceRegistry services()");
        assertThat(tracker).contains(
                "TrackedServiceRegistration",
                "serviceRegistrationsToClose",
                "registration.unregisterFromTracker()");
        assertThat(server).contains(
                "private final FandServiceRegistry services",
                "this.services = new FandServiceRegistry(permissions)",
                "this.plugins.serviceRegistry(services)",
                "public ServiceRegistry services()",
                "services.close()");
    }

    @Test
    void pluginDescriptorManifestMetadataStaysWiredToRuntimeAndDocs() throws IOException {
        var descriptor = read("../fand-api/src/main/java/io/fand/api/plugin/PluginDescriptor.java");
        var runtime = read("src/main/java/io/fand/server/plugin/PluginRuntime.java");
        var docs = read("../docs/API_DEVELOPMENT.md");

        assertThat(descriptor).contains(
                "String description",
                "String website",
                "String license",
                "String apiVersion",
                "CURRENT_API_VERSION = \"0.1.1\"");
        assertThat(runtime).contains(
                "String description",
                "String website",
                "String license",
                "String apiVersion",
                "PluginDescriptor.CURRENT_API_VERSION",
                "validateWebsite(jarPath, descriptor.website())");
        assertThat(docs).contains(
                "\"description\": \"Example Fand plugin\"",
                "\"website\": \"https://example.com\"",
                "\"license\": \"MIT\"",
                "\"apiVersion\": \"0.1.1\"");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
