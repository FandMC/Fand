package io.fand.server.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.brigadier.tree.RootCommandNode;
import io.fand.api.command.CommandDescriptor;
import io.fand.api.command.CommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class CommandTreeBridgeTest {

    @Test
    void appendsVisibleFandCommandsToRoot() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        manager.register(
                new CommandDescriptor("fand", "fand", java.util.List.of("reload"), java.util.List.of(), null),
                (sender, label, args) -> {
                },
                (sender, label, args) -> java.util.List.of()
        );

        var root = new RootCommandNode<CommandSourceStack>();
        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        var local = root.getChild("fand");
        var namespaced = root.getChild("fand:fand");
        assertThat(local).isNotNull();
        assertThat(namespaced).isNotNull();
        assertThat(local.getChild("reload")).isNotNull();
        assertThat(namespaced.getChild("reload")).isNotNull();
        assertThat(local.getChild("reload").getChild("args")).isNotNull();
    }

    @Test
    void usesDeclaredArgumentNames() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        manager.register(
                new CommandDescriptor("demo", "give", java.util.List.of(), java.util.List.of("item", "amount", "player"), java.util.List.of(), null),
                (sender, label, args) -> {
                },
                (sender, label, args) -> java.util.List.of()
        );

        var root = new RootCommandNode<CommandSourceStack>();
        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        var item = root.getChild("give").getChild("item");
        var amount = item.getChild("amount");
        var player = amount.getChild("player");
        assertThat(item).isNotNull();
        assertThat(amount).isNotNull();
        assertThat(player).isNotNull();
        assertThat(root.getChild("give").getChild("args")).isNull();
    }

    @Test
    void omitsArgumentNodeWhenNoArgumentsAreDeclared() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        manager.register(
                new CommandDescriptor("demo", "info", java.util.List.of(), java.util.List.of(), java.util.List.of(), null),
                (sender, label, args) -> {
                },
                (sender, label, args) -> java.util.List.of()
        );

        var root = new RootCommandNode<CommandSourceStack>();
        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        assertThat(root.getChild("info")).isNotNull();
        assertThat(root.getChild("info").getChildren()).isEmpty();
    }

    @Test
    void omitsAmbiguousLocalRootsButKeepsNamespacedRoots() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        manager.register(new CommandDescriptor("fand", "reload", java.util.List.of(), java.util.List.of(), null), (s, l, a) -> {}, (s, l, a) -> java.util.List.of());
        manager.register(new CommandDescriptor("tools", "reload", java.util.List.of(), java.util.List.of(), null), (s, l, a) -> {}, (s, l, a) -> java.util.List.of());

        var root = new RootCommandNode<CommandSourceStack>();
        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        assertThat(root.getChild("reload")).isNull();
        assertThat(root.getChild("fand:reload")).isNotNull();
        assertThat(root.getChild("tools:reload")).isNotNull();
    }

    private static final class TestSender implements CommandSender {
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
