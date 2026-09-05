package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.command.CommandArgumentType;
import io.fand.api.command.Arguments;
import io.fand.server.command.CommandManager;
import io.fand.server.command.CommandTreeBridge;
import io.fand.server.command.CommandTreeSynchronizer;
import io.fand.server.event.EventDispatcher;
import io.fand.server.permission.PermissionManager;
import io.fand.server.permission.PermissionSet;
import io.fand.server.scheduler.TaskScheduler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PluginCommandIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void scopedPluginCommandsPreserveTypedArguments() {
        var commands = new CommandManager(new PermissionManager());
        var registry = new PluginCommandRegistry(commands, new PluginResourceTracker(), "demo");

        registry.register("give", command -> command
                .namespace("ignored")
                .argument("target", Arguments.word(), target -> target.executes(context -> {})));

        var info = commands.lookup("demo:give").orElseThrow();
        assertThat(info.namespace()).isEqualTo("demo");
        assertThat(info.arguments()).hasSize(1);
        assertThat(info.arguments().getFirst().type()).isEqualTo(CommandArgumentType.WORD);
    }

    @Test
    void pluginAnnotatedCommandExecutesCompletesAndUnregistersOnDisable() throws Exception {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        var logFile = tempDir.resolve("commands.log");
        var previousLog = System.getProperty("fand.plugin.test.log");
        System.setProperty("fand.plugin.test.log", logFile.toString());
        try {
            PluginRuntimeTestSupport.createPluginJar(
                    tempDir,
                    pluginsDir.resolve("demo.jar"),
                    PluginRuntimeTestSupport.descriptorJson("demo", "testplugins.demo.DemoPlugin", List.of()),
                    Map.of("testplugins/demo/DemoPlugin.java", pluginSource()),
                    List.of()
            );

            var commands = new CommandManager(new PermissionManager());
            var runtime = new PluginRuntime(
                    pluginsDir,
                    pluginsDir,
                    getClass().getClassLoader(),
                    commands,
                    new EventDispatcher(),
                    new PermissionManager(),
                    new TaskScheduler()
            );
            var denied = new Sender(false);
            var allowed = new Sender(true);
            try {
                runtime.loadPlugins();
                runtime.enablePlugins();

                assertThat(commands.resolve(denied, List.of("hello", "world", "alpha"))).isEmpty();
                assertThat(commands.resolve(allowed, List.of("hello", "world", "alpha"))).isPresent();
                assertThat(commands.suggestions(allowed, List.of("hello", "w"))).containsExactly("world");
                assertThat(commands.suggestions(allowed, List.of("hello", "world", ""))).containsExactly("alpha", "beta");
                assertThat(commands.suggestions(allowed, List.of("hello", "world", "a"))).containsExactly("alpha");

                var resolved = commands.resolve(allowed, List.of("hello", "world", "alpha")).orElseThrow();
                resolved.command().execute(allowed, resolved.usedLabel(), List.of("alpha"));

                var root = new com.mojang.brigadier.tree.RootCommandNode<net.minecraft.commands.CommandSourceStack>();
                CommandTreeBridge.appendToRoot(commands, allowed, root);
                assertThat(root.getChild("hello")).isNotNull();
                assertThat(root.getChild("demo:hello")).isNotNull();
            } finally {
                runtime.close();
            }

            assertThat(commands.resolve(allowed, List.of("demo", "hello", "world"))).isEmpty();
            assertThat(Files.readAllLines(logFile)).containsExactly("command:alpha");
        } finally {
            PluginRuntimeTestSupport.restoreProperty("fand.plugin.test.log", previousLog);
        }
    }

    private static String pluginSource() {
        return """
                package testplugins.demo;

                import io.fand.api.command.Arg;
                import io.fand.api.command.Command;
                import io.fand.api.command.CommandContext;
                import io.fand.api.command.Permission;
                import io.fand.api.command.Subcommand;
                import io.fand.api.plugin.Plugin;
                import io.fand.api.plugin.PluginContext;
                import io.fand.server.command.AnnotatedCommands;
                import java.io.IOException;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.StandardOpenOption;
                import java.util.List;

                public final class DemoPlugin implements Plugin {
                    @Override
                    public void onEnable(PluginContext context) {
                        context.permissions().register(new io.fand.api.permission.PermissionDescriptor("demo.command.hello", io.fand.api.permission.PermissionDefault.FALSE));
                        context.commands().register(new HelloCommand());
                    }

                    @Command("hello")
                    @Permission("demo.command.hello")
                    public static final class HelloCommand {
                        @Subcommand("world")
                        public void world(CommandContext context, @Arg(value = "name", suggestions = {"alpha", "beta"}) String name) {
                            log("command:" + name);
                        }
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

    @Test
    void refreshesClientTreesAcrossDynamicPluginLifecycle() throws Exception {
        var pluginsDir = Files.createDirectories(tempDir.resolve("plugins"));
        var jarPath = pluginsDir.resolve("demo.jar");
        var descriptor = PluginRuntimeTestSupport.descriptorJson("demo", "testplugins.demo.DemoPlugin", List.of());
        PluginRuntimeTestSupport.createPluginJar(tempDir, jarPath, descriptor,
                Map.of("testplugins/demo/DemoPlugin.java", pluginSource()), List.of());
        var permissions = new PermissionManager();
        var commands = new CommandManager(permissions);
        var synchronizer = new CommandTreeSynchronizer(commands);
        var allowed = new Sender(true);
        var sentTrees = new ArrayList<com.mojang.brigadier.tree.RootCommandNode<net.minecraft.commands.CommandSourceStack>>();
        Runnable refresh = () -> {
            var root = new com.mojang.brigadier.tree.RootCommandNode<net.minecraft.commands.CommandSourceStack>();
            CommandTreeBridge.appendToRoot(commands, allowed, root);
            sentTrees.add(root);
        };
        try (var scheduler = new TaskScheduler();
             var runtime = new PluginRuntime(pluginsDir, pluginsDir, getClass().getClassLoader(), commands,
                     new EventDispatcher(), permissions, scheduler)) {
            runtime.loadPlugins();
            runtime.enablePlugins();
            synchronizer.tick(refresh);
            assertThat(sentTrees).hasSize(1);
            assertThat(sentTrees.getLast().getChild("hello").getChild("world").getChild("name")).isNotNull();

            var updatedSource = pluginSource().replace("@Command(\"hello\")", "@Command(\"updated\")")
                    .replace("{\"alpha\", \"beta\"}", "{\"gamma\", \"delta\"}");
            PluginRuntimeTestSupport.createPluginJar(tempDir, jarPath, descriptor,
                    Map.of("testplugins/demo/DemoPlugin.java", updatedSource), List.of());
            assertThat(runtime.reloadPlugin("demo", false).success()).isTrue();
            synchronizer.tick(refresh);
            synchronizer.tick(refresh);
            assertThat(sentTrees).hasSize(2);
            assertThat(sentTrees.getLast().getChild("hello")).isNull();
            assertThat(sentTrees.getLast().getChild("demo:hello")).isNull();
            assertThat(sentTrees.getLast().getChild("updated").getChild("world").getChild("name")).isNotNull();
            assertThat(commands.suggestions(allowed, List.of("updated", "world", ""))).containsExactly("gamma", "delta");

            assertThat(runtime.disablePlugin("demo", false).success()).isTrue();
            synchronizer.tick(refresh);
            assertThat(sentTrees).hasSize(3);
            assertThat(sentTrees.getLast().getChildren()).isEmpty();

            assertThat(runtime.enablePlugin("demo").success()).isTrue();
            synchronizer.tick(refresh);
            assertThat(sentTrees).hasSize(4);
            assertThat(sentTrees.getLast().getChild("demo:updated")).isNotNull();

            assertThat(runtime.reloadAllPlugins().success()).isTrue();
            synchronizer.tick(refresh);
            assertThat(sentTrees).hasSize(5);
            assertThat(sentTrees.getLast().getChild("updated").getChild("world").getChild("name")).isNotNull();

            assertThat(runtime.unloadPlugin("demo", false).success()).isTrue();
            synchronizer.tick(refresh);
            assertThat(sentTrees).hasSize(6);
            assertThat(sentTrees.getLast().getChildren()).isEmpty();
            assertThat(commands.suggestions(allowed, List.of(""))).isEmpty();

            assertThat(runtime.loadPlugin("demo").success()).isTrue();
            synchronizer.tick(refresh);
            assertThat(sentTrees).hasSize(7);
            assertThat(sentTrees.getLast().getChild("updated")).isNotNull();
        }
    }

    private static final class Sender implements io.fand.api.command.CommandSender, io.fand.api.permission.PermissionSubject {

        private final PermissionSet permissions;
        private final List<Component> messages = new ArrayList<>();

        private Sender(boolean allowCommand) {
            this.permissions = new PermissionSet(false);
            if (allowCommand) {
                this.permissions.set("demo.command.hello", true);
            }
        }

        @Override
        public String name() {
            return "sender";
        }

        @Override
        public void sendMessage(Component message) {
            messages.add(message);
        }

        @Override
        public boolean can(String permission) {
            return permissions.permissionValue(permission).orElse(false);
        }

        @Override
        public boolean operator() {
            return permissions.operator();
        }

        @Override
        public java.util.Optional<Boolean> permissionValue(String node) {
            return permissions.permissionValue(node);
        }
    }
}
