package io.fand.server.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
    void advertisesOnlySelectedLocalOwnerAndKeepsNamespacedRoots() {
        var manager = new CommandManager(new io.fand.server.permission.PermissionManager());
        manager.register("reload", command -> command.namespace("fand")
                .literal("first", first -> first.executes(context -> {})));
        manager.register("reload", command -> command.namespace("tools")
                .literal("second", second -> second.executes(context -> {})));

        var root = new RootCommandNode<CommandSourceStack>();
        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        assertThat(root.getChild("reload").getChild("first")).isNotNull();
        assertThat(root.getChild("reload").getChild("second")).isNull();
        assertThat(root.getChild("fand:reload")).isNotNull();
        assertThat(root.getChild("tools:reload")).isNotNull();
    }

    @Test
    void preservesLiteralOrderAfterArgumentsAndOptionalExecutability() {
        var manager = new CommandManager();
        manager.register("demo", command -> command.namespace("test")
                .argument("value", Arguments.word(), value -> value
                        .literal("confirm", confirm -> confirm
                                .argument("force", Arguments.bool().asOptional(false), force -> force.executes(context -> {})))));
        var root = new RootCommandNode<CommandSourceStack>();

        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        var demo = root.getChild("demo");
        var value = demo.getChild("value");
        var confirm = value.getChild("confirm");
        assertThat(demo.getChild("confirm")).isNull();
        assertThat(demo.getCommand()).isNull();
        assertThat(value.getCommand()).isNull();
        assertThat(confirm.getCommand()).isNotNull();
        assertThat(confirm.getChild("force").getCommand()).isNotNull();
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

    @Test
    void replacesShadowedVanillaBranchesAndRestoresThemAfterUnregister() {
        var manager = new CommandManager();
        var registration = manager.register("give", command -> command.namespace("demo")
                .literal("custom", child -> child.executes(context -> {})));
        var root = vanillaRoot();

        CommandTreeBridge.appendToRoot(manager, new TestSender(), root);

        assertThat(root.getChild("give").getChild("vanilla")).isNull();
        assertThat(root.getChild("give").getChild("custom")).isNotNull();
        assertThat(root.getChild("help")).isNotNull();

        registration.unregister();
        var refreshed = vanillaRoot();
        CommandTreeBridge.appendToRoot(manager, new TestSender(), refreshed);

        assertThat(refreshed.getChild("give").getChild("vanilla")).isNotNull();
        assertThat(refreshed.getChild("give").getChild("custom")).isNull();
        assertThat(refreshed.getChild("demo:give")).isNull();
    }

    @Test
    void hidesShadowedVanillaRootWhenPluginPermissionIsDenied() {
        var manager = new CommandManager();
        manager.register("give", command -> command.namespace("demo").permission("demo.admin")
                .executes(context -> {}));
        var root = vanillaRoot();
        var denied = new TestSender() {
            @Override
            public boolean can(String permission) {
                return false;
            }
        };

        CommandTreeBridge.appendToRoot(manager, denied, root);

        assertThat(root.getChild("give")).isNull();
        assertThat(root.getChild("demo:give")).isNull();
        assertThat(root.getChild("help")).isNotNull();
    }

    private static RootCommandNode<CommandSourceStack> vanillaRoot() {
        var root = new RootCommandNode<CommandSourceStack>();
        root.addChild(LiteralArgumentBuilder.<CommandSourceStack>literal("give")
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("vanilla").executes(context -> 1)).build());
        root.addChild(LiteralArgumentBuilder.<CommandSourceStack>literal("help").executes(context -> 1).build());
        return root;
    }

    private static class TestSender implements CommandSender {
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
