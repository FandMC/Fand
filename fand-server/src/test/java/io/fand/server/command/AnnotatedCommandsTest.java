package io.fand.server.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class AnnotatedCommandsTest {

    @Test
    void registersAnnotatedCommandInstance() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        AnnotatedCommands.register(manager, "demo", new DemoCommand());

        var resolved = manager.resolve(new Sender(), List.of("hello", "world")).orElseThrow();
        assertThat(resolved.command().descriptor().namespace()).isEqualTo("demo");
        assertThat(resolved.command().descriptor().subcommands()).containsExactly("world");
    }

    @Test
    void infersNamespaceFromPluginRegistry() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        var pluginRegistry = io.fand.server.plugin.PluginRegistries.testRegistry(manager, "demo");

        AnnotatedCommands.register(pluginRegistry, new DemoCommand());

        var resolved = manager.resolve(new Sender(), List.of("demo:hello", "world")).orElseThrow();
        assertThat(resolved.command().descriptor().namespace()).isEqualTo("demo");
    }

    @Test
    void rejectsMissingAnnotation() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        assertThatThrownBy(() -> AnnotatedCommands.register(manager, "demo", new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@CommandSpec");
    }

    @CommandSpec(label = "hello", subcommands = {"world"}, permission = "demo.hello")
    private static final class DemoCommand implements CommandExecutor {
        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
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
