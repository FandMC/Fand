# API Gap Progress

## Priority

1. BossBar service
2. Per-viewer tab list and player-info packet API
3. Ping spoof and per-world player list on top of player-info API
4. Global/proxy player list sync hooks
5. Placeholder service
6. MiniMessage/RGB/gradient parser
7. External MySQL/Redis/RabbitMQ integration strategy

## 2026-06-20

- Current focus: BossBar service.
- Finding: low-level per-player bossbar rendering already exists through `FandPlayer#showBossBar`, `FandPlayer#hideBossBar`, and `BossBarTracker`.
- Implemented: added `BossBarService`, `BossBarHandle`, and `BossBarRegistration` API surfaces.
- Implemented: added server-side keyed boss bar registration, per-player show/hide, mutable title/progress/color/overlay/flags, temporary `send(...)`, and automatic cleanup on server close.
- Implemented: added plugin-scoped boss bar service with namespace scoping and plugin lifecycle tracking.
- Verified: `./gradlew :fand-api:compileJava :fand-server:compileJava`.
- Verified: `./gradlew :fand-server:test --tests io.fand.server.plugin.PluginBossBarServiceTest`.
- Current focus: per-viewer player-info/tablist packet API.
- Finding: real-player tab display name/order and per-viewer visible/hidden tab rows already exist on `Player`.
- Finding: Minecraft 26.2 exposes public `ClientboundPlayerInfoUpdatePacket.Entry`, but only has public packet constructors from `ServerPlayer`; the initial implementation isolates custom-entry packet construction in one server helper so it can be swapped to an NMS factory later if reflection becomes unsafe.
- Implemented: added `TabListService`, `TabListEntry`, and `TabListRegistration` API surfaces.
- Implemented: added per-viewer virtual/remote tab entry add/update/remove using player-info update/remove packets.
- Implemented: added plugin lifecycle tracking for virtual tab rows.
- Implemented: added `Player#setDisplayedPing(int)` and `Player#resetDisplayedPing()` backed by persistent player-info packet rewriting, so vanilla latency refreshes no longer overwrite spoofed tab-list ping values.
- Implemented: moved real-player tab visibility state into `FandTabListService` and rewrites outbound player-info updates, so hidden tab entries are not revived by vanilla broadcasts.
- Implemented: added `TabListService#visible`, `setVisible`, and `showOnly` helpers so per-world/per-group player lists can be built without direct reverse calls on target players.
- Implemented: plugin-scoped tab visibility cleanup, so hidden real-player rows are restored when the owning plugin unloads or explicitly shows them again.
- Verified: `./gradlew :fand-server:test --tests io.fand.server.plugin.PluginTabListServiceTest`.
- Verified: `./gradlew :fand-server:test --tests io.fand.server.tablist.FandTabListPacketsTest`.
- Verified: `./gradlew :fand-api:compileJava :fand-server:compileJava`.
- Implemented: added `TabListGroup` and `TabListLayout` helpers for building per-viewer row groups from real players without exposing NMS packet details.
- Implemented: added `RemoteTabListEntry` and `TabListSyncStrategy` so proxy/cluster bridges can publish remote player-list rows through the same `TabListService` surface.
- Implemented: added public `PlayerInfoPacketFactory` through `PacketRegistry#playerInfo()`, backed by server-side player-info update/remove packet rebuilding for interceptor replacement flows.
- Remaining for this lane: concrete proxy transport/adapters are intentionally left to plugins or distribution modules; the API now exposes the stable helper and packet construction surface they need.
- Current focus: Placeholder service.
- Implemented: added `PlaceholderService`, `PlaceholderProvider`, and `PlaceholderRegistration` API surfaces.
- Implemented: added global `Server#placeholders()` and plugin-scoped `PluginContext#placeholders()`.
- Implemented: added `FandPlaceholderService` with `%identifier%` replacement, namespace provider lookup, replacement registration, and cleanup.
- Implemented: plugin placeholder registration is restricted to the plugin's namespace and is automatically unregistered on plugin unload.
- Verified: `./gradlew :fand-server:test --tests io.fand.server.placeholder.FandPlaceholderServiceTest`.
- Verified: `./gradlew :fand-server:test --tests io.fand.server.plugin.PluginPlaceholderServiceTest`.
- Note: parallel Gradle test invocations can race on `fand-server/build/test-results/test/binary`; rerun narrow tests serially when validating.
- Current focus: MiniMessage/RGB/gradient parser.
- Implemented: added `MiniMessageService` API backed by Adventure MiniMessage, including parse, serialize, escape, strip, and custom `TagResolver` access.
- Implemented: added global `Server#miniMessages()` and plugin-visible `PluginContext#miniMessages()`.
- Implemented: server and plugin contexts now replace Fand `%namespace_value%` placeholders before MiniMessage parsing, so placeholders can feed RGB/gradient/formatted components.
- Verified: `./gradlew --no-parallel --max-workers=2 :fand-api:compileJava :fand-server:compileJava`.
- Verified: `./gradlew --no-parallel --max-workers=2 :fand-server:test --tests io.fand.server.text.FandMiniMessageServiceTest`.
- Verified: `./gradlew --no-parallel --max-workers=2 :fand-server:test --tests io.fand.server.world.ApiSurfaceSourceTest`.

## 2026-06-26

- Current focus: external integration strategy and API style consistency.
- Implemented: added `ExternalIntegration`, `ExternalIntegrationKind`, and `ExternalIntegrationStrategy` API surfaces for declarative SQL/Redis/message-queue integration handles.
- Implemented: wired `Server#integrations()` and `PluginContext#integrations()` through the server/plugin runtime with an empty default strategy. Concrete MySQL, Redis, RabbitMQ, or other clients remain plugin/distribution-owned.
- Implemented: added `Player#hideEntity(Entity)` and `Player#showEntity(Entity)` receiver-style overloads, with the old `hideEntity(Player, Entity)` / `showEntity(Player, Entity)` signatures retained as deprecated compatibility forwarders.

## 2026-06-24

- Current focus: region / land API.
- Implemented: added `RegionService`, `Region`, `RegionDefinition`, `RegionFlag`, `RegionFlagCodec`, `RegionRegistration`, and `RegionFlagRegistration` API surfaces.
- Implemented: added server-side `FandRegionService` with JSON-backed region persistence under `regions/<namespace>/<path>.json`, region lookup, region removal, flag registration, and location-based region queries.
- Implemented: added plugin-scoped `PluginRegionService` wrapper and lifecycle tracking, so region registrations are cleaned up with the owning plugin.
- Implemented: wired `Server#regions()` and `PluginContext#regions()` through the runtime.
- Verified: `./gradlew.bat --no-daemon --max-workers=1 :fand-server:test --tests io.fand.server.region.FandRegionServiceTest`.

## 2026-06-25

- Current focus: chunk pipeline throughput and worldgen parallelism.
- Implemented: split chunk background work into `chunks.backgroundThreads` for load/IO work and `chunks.worldgenThreads` for terrain-generation work.
- Implemented: kept `chunks.worldgenParallelism` as the independent batch scheduler limit, so batch width and worker count can now be tuned separately.
- Implemented: moved structure-start, structure-reference, surface, carver, feature, and spawn generation onto the dedicated worldgen executor.
- Implemented: reloaded the chunk thread pools when either background or worldgen thread counts change.
- Implemented: increased the automatic worldgen worker default to use available processors up to 32 threads.
- Verified: `./gradlew \"fand-server:compileJava\" \"fand-server:compileTestJava\"`.
- Verified: `./gradlew \"fand-server:test\" --tests \"io.fand.server.config.FandConfigTest\" --tests \"io.fand.server.config.ConfigReloadResultTest\"`.
- Verified: `./gradlew \"fand-server:build\"`.

## 2026-06-21

- Current focus: block API surface and scheduler region lanes.
- Implemented: added `BlockPhysics`, `FluidType`, and `FluidTypes` API surfaces.
- Implemented: exposed block physical flags and values through `BlockType#physics()` and live-position `Block#physics()`.
- Implemented: added `Block#fluid()`, `relative(...)`, `drops(...)`, `breakNaturally(...)`, and `applyPhysics()`.
- Implemented: added `Scheduler#region()` with an independent region worker scheduler: tasks in the same 8x8 chunk region are serialized, while different regions can run on different lanes.
- Implemented: added `scheduler.regionThreads` startup config and plugin lifecycle tracking for region tasks.
- Verified: `./gradlew.bat --no-daemon --max-workers=1 :fand-server:test --tests io.fand.server.scheduler.TaskSchedulerTest --tests io.fand.server.config.FandConfigTest --tests io.fand.server.ConfigReloadResultTest --tests io.fand.server.plugin.PluginCleanupTest`.
- Current focus: fluid-friendly block API.
- Implemented: added `FluidState` snapshots with source/full/falling/amount/height/blast-resistance/flow-vector data and water/lava/flowing predicates.
- Implemented: added `Block#fluidState()`, fluid convenience checks, `setFluid(...)`, and `clearFluid()`.
- Implemented: server-side fluid placement uses vanilla waterlogging containers when possible, otherwise places the fluid's legacy block state; clearing waterlogged blocks preserves the host block.
- Verified: `./gradlew.bat --no-daemon --max-workers=1 :fand-api:test --tests io.fand.api.ApiGapModelsTest :fand-server:compileJava`.
