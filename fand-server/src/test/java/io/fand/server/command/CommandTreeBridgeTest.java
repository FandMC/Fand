package io.fand.server.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.fand.api.command.Arguments;
import io.fand.api.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

final class CommandTreeBridgeTest {

    @Test
    void appendsVisibleFandCommandsToRoot() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        manager.register("fand", command -> command
                .namespace("fand")
                .literal("reload", reload -> reload.executes(context -> {})));

        var root = new RootCommandNode<CommandSourceStack>();
        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        var local = root.getChild("fand");
        var namespaced = root.getChild("fand:fand");
        assertThat(local).isNotNull();
        assertThat(namespaced).isNotNull();
        assertThat(local.getChild("reload")).isNotNull();
        assertThat(namespaced.getChild("reload")).isNotNull();
    }

    @Test
    void usesDeclaredArgumentNames() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        manager.register("give", command -> command
                .namespace("demo")
                .argument("item", Arguments.word(), item -> item
                        .argument("amount", Arguments.integer(), amount -> amount
                                .argument("player", Arguments.player(), player -> player.executes(context -> {})))));

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
        manager.register("info", command -> command.namespace("demo").executes(context -> {}));

        var root = new RootCommandNode<CommandSourceStack>();
        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        assertThat(root.getChild("info")).isNotNull();
        assertThat(root.getChild("info").getChildren()).isEmpty();
    }

    @Test
    void omitsAmbiguousLocalRootsButKeepsNamespacedRoots() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        manager.register("reload", command -> command.namespace("fand").executes(context -> {}));
        manager.register("reload", command -> command.namespace("tools").executes(context -> {}));

        var root = new RootCommandNode<CommandSourceStack>();
        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        assertThat(root.getChild("reload")).isNull();
        assertThat(root.getChild("fand:reload")).isNotNull();
        assertThat(root.getChild("tools:reload")).isNotNull();
    }

    @Test
    void appendsTypedBuilderArguments() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        manager.register("give", command -> command
                .namespace("demo")
                .argument("target", Arguments.player(), target -> target
                        .argument("amount", Arguments.integer(1, 64), amount -> amount
                                .executes(context -> {
                                }))));

        var root = new RootCommandNode<CommandSourceStack>();
        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        var target = root.getChild("give").getChild("target");
        var amount = target.getChild("amount");
        assertThat(((ArgumentCommandNode<?, ?>) target).getType().getClass().getName()).contains("EntityArgument");
        assertThat(((ArgumentCommandNode<?, ?>) amount).getType().getClass().getName()).contains("IntegerArgumentType");
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
        public boolean can(String permission) {
            return true;
        }
    }
}
