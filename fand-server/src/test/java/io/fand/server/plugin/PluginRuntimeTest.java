package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.lifecycle.PluginDisableEvent;
import io.fand.api.lifecycle.PluginEnableEvent;
import io.fand.api.loot.LootContext;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.server.command.CommandManager;
import io.fand.server.event.EventDispatcher;
import io.fand.server.loot.FandLootTableService;
import io.fand.server.permission.PermissionManager;
import io.fand.server.scheduler.TaskScheduler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PluginRuntimeTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsEnablesAndDisablesPluginsInDependencyOrder() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        var logFile = tempDir.resolve("plugin.log");
        var previousLog = System.getProperty("fand.plugin.test.log");
        System.setProperty("fand.plugin.test.log", logFile.toString());
        try {
            var baseJar = PluginRuntimeTestSupport.createPluginJar(
                    tempDir,
                    pluginsDir.resolve("base.jar"),
                    PluginRuntimeTestSupport.descriptorJson("base", "testplugins.base.BasePlugin", List.of()),
                    Map.of(
                            "testplugins/base/BasePlugin.java",
                            pluginSource(
                                    "testplugins.base",
                                    "BasePlugin",
                                    "log(\"base-load\");",
                                    "log(\"base-enable\");",
                                    "log(\"base-disable\");"
                            ),
                            "testplugins/base/SharedGreeter.java",
                            "package testplugins.base; public final class SharedGreeter { public static String message() { return \"shared\"; } }"
                    ),
                    List.of()
            );
            PluginRuntimeTestSupport.createPluginJar(
                    tempDir,
                    pluginsDir.resolve("dependent.jar"),
                    PluginRuntimeTestSupport.descriptorJson("dependent", "testplugins.dependent.DependentPlugin", List.of("base")),
                    Map.of("testplugins/dependent/DependentPlugin.java", dependentPluginSource()),
                    List.of(baseJar)
            );

            var manager = new PluginRuntime(pluginsDir, pluginsDir, getClass().getClassLoader(), new CommandManager(), new EventDispatcher(), new PermissionManager(), new TaskScheduler());
            try {
                manager.loadPlugins();
                assertThat(manager.loaded()).hasSize(2);
                assertThat(manager.byId("base")).isPresent();
                assertThat(manager.byId("dependent")).isPresent();
                assertThat(manager.isEnabled("base")).isFalse();

                manager.enablePlugins();
                assertThat(manager.isEnabled("base")).isTrue();
                assertThat(manager.isEnabled("dependent")).isTrue();
            } finally {
                manager.close();
            }

            assertThat(Files.readAllLines(logFile)).containsExactly(
                    "base-load",
                    "dependent-load",
                    "base-enable",
                    "dependent-enable:shared",
                    "dependent-disable",
                    "base-disable"
            );
            assertThat(pluginsDir.resolve("base")).isDirectory();
            assertThat(pluginsDir.resolve("dependent")).isDirectory();
        } finally {
            PluginRuntimeTestSupport.restoreProperty("fand.plugin.test.log", previousLog);
        }
    }

    @Test
    void rejectsMissingDependencyWhenConfiguredStrictly() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("broken.jar"),
                PluginRuntimeTestSupport.descriptorJson("broken", "testplugins.broken.BrokenPlugin", List.of("missing")),
                Map.of("testplugins/broken/BrokenPlugin.java", pluginSource("testplugins.broken", "BrokenPlugin", null, null, null)),
                List.of()
        );

        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                new EventDispatcher(),
                new PermissionManager(),
                new TaskScheduler(),
                new PluginRuntime.Options(false, false, false)
        );
        assertThatThrownBy(manager::loadPlugins)
                .isInstanceOf(PluginLoadException.class)
                .hasMessageContaining("depends on missing plugin 'missing'");
    }

    @Test
    void skipsMissingDependencyByDefault() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("broken.jar"),
                PluginRuntimeTestSupport.descriptorJson("broken", "testplugins.broken.BrokenPlugin", List.of("missing")),
                Map.of("testplugins/broken/BrokenPlugin.java", pluginSource("testplugins.broken", "BrokenPlugin", null, null, null)),
                List.of()
        );

        var manager = new PluginRuntime(pluginsDir, pluginsDir, getClass().getClassLoader(), new CommandManager(), new EventDispatcher(), new PermissionManager(), new TaskScheduler());
        try {
            manager.loadPlugins();

            assertThat(manager.loaded()).isEmpty();
            assertThat(manager.byId("broken")).isEmpty();
        } finally {
            manager.close();
        }
    }

    @Test
    void rejectsDuplicatePluginIdsWhenConfiguredStrictly() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("first.jar"),
                PluginRuntimeTestSupport.descriptorJson("dup", "testplugins.first.FirstPlugin", List.of()),
                Map.of("testplugins/first/FirstPlugin.java", pluginSource("testplugins.first", "FirstPlugin", null, null, null)),
                List.of()
        );
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("second.jar"),
                PluginRuntimeTestSupport.descriptorJson("dup", "testplugins.second.SecondPlugin", List.of()),
                Map.of("testplugins/second/SecondPlugin.java", pluginSource("testplugins.second", "SecondPlugin", null, null, null)),
                List.of()
        );

        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                new EventDispatcher(),
                new PermissionManager(),
                new TaskScheduler(),
                new PluginRuntime.Options(false, false, false)
        );
        assertThatThrownBy(manager::loadPlugins)
                .isInstanceOf(PluginLoadException.class)
                .hasMessageContaining("Duplicate plugin id 'dup'");
    }

    @Test
    void skipsDuplicatePluginIdsByDefault() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("first.jar"),
                PluginRuntimeTestSupport.descriptorJson("dup", "testplugins.first.FirstPlugin", List.of()),
                Map.of("testplugins/first/FirstPlugin.java", pluginSource("testplugins.first", "FirstPlugin", null, null, null)),
                List.of()
        );
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("second.jar"),
                PluginRuntimeTestSupport.descriptorJson("dup", "testplugins.second.SecondPlugin", List.of()),
                Map.of("testplugins/second/SecondPlugin.java", pluginSource("testplugins.second", "SecondPlugin", null, null, null)),
                List.of()
        );

        var manager = new PluginRuntime(pluginsDir, pluginsDir, getClass().getClassLoader(), new CommandManager(), new EventDispatcher(), new PermissionManager(), new TaskScheduler());
        try {
            manager.loadPlugins();

            assertThat(manager.loaded()).hasSize(1);
            assertThat(manager.byId("dup")).isPresent();
        } finally {
            manager.close();
        }
    }

    @Test
    void continuesAfterLoadFailureWhenConfigured() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);

        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("broken.jar"),
                PluginRuntimeTestSupport.descriptorJson("broken", "testplugins.broken.BrokenPlugin", List.of()),
                Map.of("testplugins/broken/BrokenPlugin.java", "package testplugins.broken; import io.fand.api.plugin.Plugin; public final class BrokenPlugin implements Plugin { public BrokenPlugin() { throw new RuntimeException(\"boom\"); } @Override public void onEnable(io.fand.api.plugin.PluginContext context) {} }") ,
                List.of()
        );
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("dependent.jar"),
                PluginRuntimeTestSupport.descriptorJson("dependent", "testplugins.dependent.DependentPlugin", List.of("broken")),
                Map.of("testplugins/dependent/DependentPlugin.java", pluginSource("testplugins.dependent", "DependentPlugin", null, null, null)),
                List.of()
        );
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("healthy.jar"),
                PluginRuntimeTestSupport.descriptorJson("healthy", "testplugins.healthy.HealthyPlugin", List.of()),
                Map.of("testplugins/healthy/HealthyPlugin.java", pluginSource("testplugins.healthy", "HealthyPlugin", null, null, null)),
                List.of()
        );

        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                new EventDispatcher(),
                new PermissionManager(),
                new TaskScheduler(),
                new PluginRuntime.Options(true, false, false)
        );
        try {
            manager.loadPlugins();

            assertThat(manager.byId("broken")).isEmpty();
            assertThat(manager.byId("dependent")).isEmpty();
            assertThat(manager.byId("healthy")).isPresent();
        } finally {
            manager.close();
        }
    }

    @Test
    void continuesAfterEnableFailureWhenConfigured() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);

        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("base.jar"),
                PluginRuntimeTestSupport.descriptorJson("base", "testplugins.base.BasePlugin", List.of()),
                Map.of("testplugins/base/BasePlugin.java", "package testplugins.base; import io.fand.api.plugin.Plugin; import io.fand.api.plugin.PluginContext; public final class BasePlugin implements Plugin { @Override public void onEnable(PluginContext context) { throw new RuntimeException(\"enable\"); } }") ,
                List.of()
        );
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("dependent.jar"),
                PluginRuntimeTestSupport.descriptorJson("dependent", "testplugins.dependent.DependentPlugin", List.of("base")),
                Map.of("testplugins/dependent/DependentPlugin.java", pluginSource("testplugins.dependent", "DependentPlugin", null, null, null)),
                List.of()
        );
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("healthy.jar"),
                PluginRuntimeTestSupport.descriptorJson("healthy", "testplugins.healthy.HealthyPlugin", List.of()),
                Map.of("testplugins/healthy/HealthyPlugin.java", pluginSource("testplugins.healthy", "HealthyPlugin", null, null, null)),
                List.of()
        );

        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                new EventDispatcher(),
                new PermissionManager(),
                new TaskScheduler(),
                new PluginRuntime.Options(false, true, false)
        );
        try {
            manager.loadPlugins();
            manager.enablePlugins();

            assertThat(manager.isEnabled("base")).isFalse();
            assertThat(manager.isEnabled("dependent")).isFalse();
            assertThat(manager.isEnabled("healthy")).isTrue();
            assertThat(manager.byId("base")).isEmpty();
            assertThat(manager.byId("dependent")).isEmpty();
            assertThat(manager.byId("healthy")).isPresent();
            assertThat(manager.loaded()).containsExactly(manager.byId("healthy").orElseThrow());
        } finally {
            manager.close();
        }
    }

    @Test
    void pluginListenerFailuresDoNotEscapePluginEventBus() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);

        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("listener.jar"),
                PluginRuntimeTestSupport.descriptorJson("listener", "testplugins.listener.ListenerPlugin", List.of()),
                Map.of("testplugins/listener/ListenerPlugin.java", """
                        package testplugins.listener;

                        import io.fand.api.plugin.Plugin;
                        import io.fand.api.plugin.PluginContext;
                        import io.fand.server.plugin.CleanupTestEvent;

                        public final class ListenerPlugin implements Plugin {
                            @Override
                            public void onEnable(PluginContext context) {
                                context.events().subscribe(CleanupTestEvent.class, event -> {
                                    throw new RuntimeException("listener failed");
                                });
                            }
                        }
                        """),
                List.of()
        );

        var dispatcher = new EventDispatcher();
        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                dispatcher,
                new PermissionManager(),
                new TaskScheduler()
        );
        try {
            manager.loadPlugins();
            manager.enablePlugins();

            assertThat(manager.isEnabled("listener")).isTrue();
            assertThatCode(() -> dispatcher.fire(new CleanupTestEvent())).doesNotThrowAnyException();
        } finally {
            manager.close();
        }
    }

    @Test
    void firesPluginEnableAndDisableEvents() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("events.jar"),
                PluginRuntimeTestSupport.descriptorJson("events", "testplugins.events.EventsPlugin", List.of()),
                Map.of("testplugins/events/EventsPlugin.java", pluginSource("testplugins.events", "EventsPlugin", null, null, null)),
                List.of()
        );

        var dispatcher = new EventDispatcher();
        var seen = new java.util.ArrayList<String>();
        dispatcher.subscribe(PluginEnableEvent.class, event -> seen.add("enable:" + event.plugin().id()));
        dispatcher.subscribe(PluginDisableEvent.class, event -> seen.add("disable:" + event.plugin().id()));
        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                dispatcher,
                new PermissionManager(),
                new TaskScheduler()
        );
        try {
            manager.loadPlugins();
            manager.enablePlugins();
        } finally {
            manager.close();
        }

        assertThat(seen).containsExactly("enable:events", "disable:events");
    }

    @Test
    void firesPluginDisableEventWhenOnDisableFails() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("broken-disable.jar"),
                PluginRuntimeTestSupport.descriptorJson("broken-disable", "testplugins.brokendisable.BrokenDisablePlugin", List.of()),
                Map.of("testplugins/brokendisable/BrokenDisablePlugin.java", """
                        package testplugins.brokendisable;

                        import io.fand.api.plugin.Plugin;
                        import io.fand.api.plugin.PluginContext;

                        public final class BrokenDisablePlugin implements Plugin {
                            @Override
                            public void onEnable(PluginContext context) {
                            }

                            @Override
                            public void onDisable(PluginContext context) {
                                throw new RuntimeException("disable");
                            }
                        }
                        """),
                List.of()
        );

        var dispatcher = new EventDispatcher();
        var seen = new java.util.ArrayList<String>();
        dispatcher.subscribe(PluginDisableEvent.class, event -> seen.add(event.plugin().id()));
        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                dispatcher,
                new PermissionManager(),
                new TaskScheduler()
        );
        try {
            manager.loadPlugins();
            manager.enablePlugins();
        } finally {
            manager.close();
        }

        assertThat(seen).containsExactly("broken-disable");
    }

    @Test
    void closesPermissionAttachmentsOwnedByPlugins() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("permissions.jar"),
                PluginRuntimeTestSupport.descriptorJson("permissions", "testplugins.permissions.PermissionsPlugin", List.of()),
                Map.of("testplugins/permissions/PermissionsPlugin.java", """
                        package testplugins.permissions;

                        import io.fand.api.plugin.Plugin;
                        import io.fand.api.plugin.PluginContext;
                        import io.fand.server.plugin.PermissionAttachmentTestBridge;

                        public final class PermissionsPlugin implements Plugin {
                            @Override
                            public void onLoad(PluginContext context) {
                                PermissionAttachmentTestBridge.attach(context);
                            }

                            @Override
                            public void onEnable(PluginContext context) {
                            }
                        }
                        """),
                List.of()
        );

        var permissions = new PermissionManager();
        permissions.register(new PermissionDescriptor("fand.injected", PermissionDefault.FALSE));
        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                new EventDispatcher(),
                permissions,
                new TaskScheduler()
        );

        manager.loadPlugins();
        assertThat(permissions.hasPermission(PermissionAttachmentTestBridge.SUBJECT, "fand.injected")).isTrue();

        manager.close();
        assertThat(permissions.hasPermission(PermissionAttachmentTestBridge.SUBJECT, "fand.injected")).isFalse();
    }

    @Test
    void scopesAndClosesLootTableReplacementsOwnedByPlugins() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("loot.jar"),
                PluginRuntimeTestSupport.descriptorJson("loot", "testplugins.loot.LootPlugin", List.of()),
                Map.of("testplugins/loot/LootPlugin.java", """
                        package testplugins.loot;

                        import io.fand.api.plugin.Plugin;
                        import io.fand.api.plugin.PluginContext;
                        import net.kyori.adventure.key.Key;

                        public final class LootPlugin implements Plugin {
                            @Override
                            public void onLoad(PluginContext context) {
                                context.lootTables().replace(Key.key("minecraft:chests/simple_dungeon"), ignored -> java.util.List.of());
                            }

                            @Override
                            public void onEnable(PluginContext context) {
                            }
                        }
                        """),
                List.of()
        );

        var lootTables = new FandLootTableService(() -> null);
        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                new EventDispatcher(),
                new PermissionManager(),
                new TaskScheduler(),
                new io.fand.server.recipe.FandRecipeRegistry(),
                lootTables,
                new io.fand.server.scoreboard.FandScoreboardService(() -> {
                    throw new IllegalStateException("Minecraft server is not attached");
                }),
                new io.fand.server.network.packet.PacketRegistryImpl(),
                io.fand.api.messaging.PluginMessaging.empty(),
                io.fand.api.advancement.AdvancementRegistry.empty(),
                io.fand.api.enchantment.EnchantmentRegistry.empty(),
                io.fand.api.structure.StructureService.empty(),
                io.fand.api.map.MapService.empty(),
                new io.fand.server.item.FandCustomItemRegistry(),
                new io.fand.server.block.FandCustomBlockRegistry(new EventDispatcher(), new io.fand.server.item.FandCustomItemRegistry()),
                new io.fand.server.gui.FandGuiService(new EventDispatcher()),
                new PluginRuntime.Options(false, false, false)
        );

        manager.loadPlugins();
        assertThat(lootTables.table(Key.key("loot:chests/simple_dungeon"))).isPresent();
        assertThat(lootTables.generate(Key.key("loot:chests/simple_dungeon"), LootContext.empty())).isEmpty();
        assertThat(lootTables.table(Key.key("minecraft:chests/simple_dungeon"))).isEmpty();

        manager.close();
        assertThat(lootTables.table(Key.key("loot:chests/simple_dungeon"))).isEmpty();
    }

    @Test
    void registersPermissionsDeclaredByPluginDescriptor() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("perms.jar"),
                PluginRuntimeTestSupport.descriptorJson(
                        "perms",
                        "testplugins.perms.PermsPlugin",
                        List.of(),
                        """
                                [
                                  {
                                    "node": "perms.admin",
                                    "defaultAccess": "FALSE",
                                    "children": {
                                      "perms.command.reload": true,
                                      "perms.command.danger": false
                                    }
                                  }
                                ]
                                """
                ),
                Map.of("testplugins/perms/PermsPlugin.java", pluginSource("testplugins.perms", "PermsPlugin", null, null, null)),
                List.of()
        );

        var permissions = new PermissionManager();
        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                new EventDispatcher(),
                permissions,
                new TaskScheduler(),
                new PluginRuntime.Options(false, false, false)
        );
        try {
            manager.loadPlugins();

            var subject = new io.fand.server.permission.PermissionSet(false).set("perms.admin", true);
            assertThat(permissions.lookup("perms.admin")).isPresent();
            assertThat(permissions.hasPermission(subject, "perms.command.reload")).isTrue();
            assertThat(permissions.hasPermission(subject, "perms.command.danger")).isFalse();
        } finally {
            manager.close();
        }
    }

    @Test
    void rejectsPluginDescriptorPermissionsOutsidePluginNamespace() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("perms.jar"),
                PluginRuntimeTestSupport.descriptorJson(
                        "perms",
                        "testplugins.perms.PermsPlugin",
                        List.of(),
                        """
                                [
                                  {
                                    "node": "other.admin",
                                    "defaultAccess": "FALSE"
                                  }
                                ]
                                """
                ),
                Map.of("testplugins/perms/PermsPlugin.java", pluginSource("testplugins.perms", "PermsPlugin", null, null, null)),
                List.of()
        );

        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                new EventDispatcher(),
                new PermissionManager(),
                new TaskScheduler(),
                new PluginRuntime.Options(false, false, false)
        );

        assertThatThrownBy(manager::loadPlugins)
                .isInstanceOf(PluginLoadException.class)
                .hasMessageContaining("invalid permission declaration");
    }

    @Test
    void rejectsRuntimePermissionRegistrationOutsidePluginNamespace() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("perms.jar"),
                PluginRuntimeTestSupport.descriptorJson("perms", "testplugins.perms.PermsPlugin", List.of()),
                Map.of("testplugins/perms/PermsPlugin.java", """
                        package testplugins.perms;

                        import io.fand.api.permission.PermissionDefault;
                        import io.fand.api.permission.PermissionDescriptor;
                        import io.fand.api.plugin.Plugin;
                        import io.fand.api.plugin.PluginContext;

                        public final class PermsPlugin implements Plugin {
                            @Override
                            public void onLoad(PluginContext context) {
                                context.permissions().register(new PermissionDescriptor("other.admin", PermissionDefault.FALSE));
                            }

                            @Override
                            public void onEnable(PluginContext context) {
                            }
                        }
                        """),
                List.of()
        );

        var manager = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                new EventDispatcher(),
                new PermissionManager(),
                new TaskScheduler(),
                new PluginRuntime.Options(false, false, false)
        );

        assertThatThrownBy(manager::loadPlugins)
                .isInstanceOf(PluginLoadException.class)
                .hasMessageContaining("Failed to load plugin 'perms'");
    }

    @Test
    void closesResourcesRegisteredDuringLoadWhenEnableFailsAndBootContinues() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        var logFile = tempDir.resolve("enable-failure-cleanup.log");
        var previousLog = System.getProperty("fand.plugin.test.log");
        System.setProperty("fand.plugin.test.log", logFile.toString());
        try {
            PluginRuntimeTestSupport.createPluginJar(
                    tempDir,
                    pluginsDir.resolve("broken.jar"),
                    PluginRuntimeTestSupport.descriptorJson("broken", "testplugins.broken.BrokenPlugin", List.of()),
                    Map.of("testplugins/broken/BrokenPlugin.java", loadResourceThenFailEnablePluginSource()),
                    List.of()
            );

            var dispatcher = new EventDispatcher();
            var scheduler = new TaskScheduler();
            var manager = new PluginRuntime(
                    pluginsDir,
                    pluginsDir,
                    getClass().getClassLoader(),
                    new CommandManager(),
                    dispatcher,
                    new PermissionManager(),
                    scheduler,
                    new PluginRuntime.Options(false, true, false)
            );
            try {
                manager.loadPlugins();
                manager.enablePlugins();

                dispatcher.fire(new CleanupTestEvent());
                scheduler.tick();

                assertThat(manager.byId("broken")).isEmpty();
                assertThat(manager.isEnabled("broken")).isFalse();
                assertThat(manager.loaded()).isEmpty();
                assertThat(Files.exists(logFile)).isFalse();
            } finally {
                manager.close();
                scheduler.close();
            }
        } finally {
            PluginRuntimeTestSupport.restoreProperty("fand.plugin.test.log", previousLog);
        }
    }

    private static String pluginSource(String packageName, String className, String loadLine, String enableLine, String disableLine) {
        var lines = new java.util.LinkedHashMap<String, String>();
        if (loadLine != null) {
            lines.put("onLoad", loadLine);
        }
        if (enableLine != null) {
            lines.put("onEnable", enableLine + "\ncontext.dataDirectory();");
        } else {
            lines.put("onEnable", "context.dataDirectory();");
        }
        if (disableLine != null) {
            lines.put("onDisable", disableLine);
        }

        var methods = new StringBuilder();
        for (var entry : lines.entrySet()) {
            var signature = switch (entry.getKey()) {
                case "onLoad" -> "public void onLoad(PluginContext context)";
                case "onEnable" -> "public void onEnable(PluginContext context)";
                case "onDisable" -> "public void onDisable(PluginContext context)";
                default -> throw new IllegalStateException(entry.getKey());
            };
            methods.append("    @Override\n")
                    .append("    ").append(signature).append(" {\n")
                    .append(entry.getValue().lines().map(line -> "        " + line).collect(java.util.stream.Collectors.joining("\n")))
                    .append("\n    }\n\n");
        }

        return """
                package %s;

                import io.fand.api.plugin.Plugin;
                import io.fand.api.plugin.PluginContext;
                import java.io.IOException;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.StandardOpenOption;

                public final class %s implements Plugin {
                %s    private static void log(String value) {
                        try {
                            Files.writeString(
                                    Path.of(System.getProperty("fand.plugin.test.log")),
                                    value + System.lineSeparator(),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.APPEND
                            );
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
                """.formatted(packageName, className, methods);
    }

    private static String dependentPluginSource() {
        return """
                package testplugins.dependent;

                import io.fand.api.plugin.Plugin;
                import io.fand.api.plugin.PluginContext;
                import java.io.IOException;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.StandardOpenOption;
                import testplugins.base.SharedGreeter;

                public final class DependentPlugin implements Plugin {
                    @Override
                    public void onLoad(PluginContext context) {
                        log("dependent-load");
                    }

                    @Override
                    public void onEnable(PluginContext context) {
                        log("dependent-enable:" + SharedGreeter.message());
                        context.dataDirectory();
                    }

                    @Override
                    public void onDisable(PluginContext context) {
                        log("dependent-disable");
                    }

                    private static void log(String value) {
                        try {
                            Files.writeString(
                                    Path.of(System.getProperty("fand.plugin.test.log")),
                                    value + System.lineSeparator(),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.APPEND
                            );
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
                """;
    }

    private static String loadResourceThenFailEnablePluginSource() {
        return """
                package testplugins.broken;

                import io.fand.api.plugin.Plugin;
                import io.fand.api.plugin.PluginContext;
                import io.fand.server.plugin.CleanupTestEvent;
                import java.io.IOException;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.StandardOpenOption;
                import java.time.Duration;

                public final class BrokenPlugin implements Plugin {
                    @Override
                    public void onLoad(PluginContext context) {
                        context.events().subscribe(CleanupTestEvent.class, event -> log("event"));
                        context.scheduler().runMainRepeating(() -> log("task"), Duration.ZERO, Duration.ofDays(1));
                    }

                    @Override
                    public void onEnable(PluginContext context) {
                        throw new RuntimeException("enable");
                    }

                    private static void log(String value) {
                        try {
                            Files.writeString(
                                    Path.of(System.getProperty("fand.plugin.test.log")),
                                    value + System.lineSeparator(),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.APPEND
                            );
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
                """;
    }
}
