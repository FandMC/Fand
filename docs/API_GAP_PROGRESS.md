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
- Remaining for this lane: layout/group helpers, global/proxy player-list sync, and public packet-level player-info factory remain to be layered on top of the now-stable per-viewer row/ping foundation.
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
