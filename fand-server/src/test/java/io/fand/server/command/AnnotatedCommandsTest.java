package io.fand.server.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.command.Aliases;
import io.fand.api.command.Arg;
import io.fand.api.command.Command;
import io.fand.api.command.CommandContext;
import io.fand.api.command.CommandSender;
import io.fand.api.command.Default;
import io.fand.api.command.Permission;
import io.fand.api.command.Subcommand;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class AnnotatedCommandsTest {

    @Test
    void registersAnnotatedCommandInstance() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        AnnotatedCommands.register(manager, "demo", new DemoCommand());

        var resolved = manager.resolve(new Sender(), List.of("hello", "world", "steve")).orElseThrow();
        assertThat(resolved.command().info().namespace()).isEqualTo("demo");
        assertThat(resolved.command().info().path()).containsExactly("world");
        assertThat(resolved.command().info().arguments()).extracting("name").containsExactly("target");
    }

    @Test
    void infersNamespaceFromPluginRegistry() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        var pluginRegistry = io.fand.server.plugin.PluginRegistries.testRegistry(manager, "demo");

        AnnotatedCommands.register(pluginRegistry, new DemoCommand());

        var resolved = manager.resolve(new Sender(), List.of("demo:hello", "world", "steve")).orElseThrow();
        assertThat(resolved.command().info().namespace()).isEqualTo("demo");
    }

    @Test
    void rejectsMissingAnnotation() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        assertThatThrownBy(() -> AnnotatedCommands.register(manager, "demo", new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Command");
    }

    @Test
    void registersBuiltinPluginCommandsWithExpectedAliases() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());

        AnnotatedCommands.register(manager, new PluginsCommand(null));
        new PluginCommand(null).register(manager);

        var plugins = manager.lookup("plugins").orElseThrow();
        assertThat(plugins.namespace()).isEqualTo("fand");
        assertThat(plugins.aliases()).containsExactly("pl");
        assertThat(plugins.permission()).isEqualTo("fand.command.plugins");
        assertThat(manager.lookup("pl")).isPresent();

        var plugin = manager.lookup("plugin").orElseThrow();
        assertThat(plugin.namespace()).isEqualTo("fand");
        assertThat(plugin.aliases()).isEmpty();
        assertThat(plugin.permission()).isNull();
    }

    @Test
    void registersNewAnnotatedCommandMethods() throws Exception {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        var command = new StructuredCommand();

        AnnotatedCommands.register(manager, "demo", command);

        var sender = new Sender();
        var root = manager.resolve(sender, List.of("tool")).orElseThrow();
        root.command().execute(sender, root.usedLabel(), List.of());
        var reload = manager.resolve(sender, List.of("tool", "reload", "3")).orElseThrow();
        reload.command().execute(sender, reload.usedLabel(), List.of("3"));

        assertThat(command.executed).containsExactly("help:tool", "reload:3");
        assertThat(manager.suggestions(sender, List.of("tool", "r"))).containsExactly("reload");
    }

    @Command("hello")
    @Permission("demo.hello")
    private static final class DemoCommand {
        @Subcommand("world")
        void world(@Arg("target") String target) {
        }
    }

    @Command("tool")
    @Aliases("tools")
    @Permission("demo.tool")
    private static final class StructuredCommand {
        private final java.util.ArrayList<String> executed = new java.util.ArrayList<>();

        @Default
        void help(CommandContext context) {
            executed.add("help:" + context.label());
        }

        @Subcommand("reload")
        void reload(@Arg(value = "times", min = 1, max = 5) int times) {
            executed.add("reload:" + times);
        }
    }

    private static final class Sender implements CommandSender {
        @Override
        public String name() {
            return "test";
        }

        @Override
        public void sendMessage(Component message) {
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }
    }
}
