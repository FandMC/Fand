package io.fand.server.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.command.Arguments;
import io.fand.api.command.CommandArgumentType;
import io.fand.api.command.CommandBuilder;
import io.fand.api.command.CommandContext;
import io.fand.api.command.CommandSender;
import io.fand.api.permission.PermissionSubject;
import io.fand.server.permission.PermissionManager;
import io.fand.server.permission.PermissionSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class CommandManagerTest {

    @Test
    void resolvesUniqueLocalAndNamespacedCommands() {
        var manager = new CommandManager(new PermissionManager());
        manager.register("reload", command -> command.namespace("fand").executes(context -> {}));

        var sender = new TestSender();
        assertThat(manager.lookup("reload")).isPresent();
        assertThat(manager.resolve(sender, List.of("reload"))).isPresent();
        assertThat(manager.resolve(sender, List.of("fand:reload"))).isPresent();
    }

    @Test
    void treatsConflictingLocalRootsAsNamespacedOnly() {
        var manager = new CommandManager(new PermissionManager());
        manager.register("reload", command -> command.namespace("fand").executes(context -> {}));
        manager.register("reload", command -> command.namespace("tools").executes(context -> {}));

        var sender = new TestSender();
        assertThat(manager.lookup("reload")).isEmpty();
        assertThat(manager.resolve(sender, List.of("reload"))).isEmpty();
        assertThat(manager.resolve(sender, List.of("fand:reload"))).isPresent();
        assertThat(manager.resolve(sender, List.of("tools:reload"))).isPresent();
    }

    @Test
    void respectsPermissionsForResolutionAndSuggestions() {
        var manager = new CommandManager(new PermissionManager());
        manager.register("config", command -> command
                .namespace("fand")
                .literal("reload", reload -> reload
                        .permission("fand.admin")
                        .executes(context -> {})));

        var denied = new TestSender();
        var allowed = new TestSender("fand.admin");

        assertThat(manager.resolve(denied, List.of("fand:config", "reload"))).isEmpty();
        assertThat(manager.resolve(allowed, List.of("fand:config", "reload"))).isPresent();
        assertThat(manager.suggestions(denied, List.of("fand:c"))).isEmpty();
        assertThat(manager.suggestions(allowed, List.of("fand:c"))).contains("fand:config");
    }

    @Test
    void resolvesSubcommandsAndUsesSeparateSuggestions() throws Exception {
        var manager = new CommandManager(new PermissionManager());
        var executed = new ArrayList<String>();
        manager.register("config", command -> command
                .namespace("fand")
                .literal("reload", reload -> reload
                        .argument("mode", Arguments.word().asOptional().suggests("suggested"), mode -> mode
                                .executes(context -> executed.add(context.label() + ":" + String.join(",", context.args()))))));

        var sender = new TestSender();
        var resolved = manager.resolve(sender, List.of("config", "reload", "now")).orElseThrow();
        resolved.command().execute(sender, resolved.usedLabel(), List.of("now"));

        assertThat(executed).containsExactly("config:now");
        assertThat(manager.suggestions(sender, List.of("config", "r"))).containsExactly("reload");
        assertThat(manager.suggestions(sender, List.of("fand:config", "r"))).containsExactly("reload");
        assertThat(manager.suggestions(sender, List.of("config", "reload", "s"))).containsExactly("suggested");
        assertThat(manager.suggestions(sender, List.of("fand:config", "reload", "s"))).containsExactly("suggested");
    }

    @Test
    void claimsOnlyCompleteRootsForExecution() {
        var manager = new CommandManager(new PermissionManager());
        manager.register("tps", command -> command.namespace("fand").executes(context -> {}));
        manager.register("config", command -> command.namespace("fand").literal("reload", reload -> reload.executes(context -> {})));

        assertThat(manager.claims(List.of("tp"))).isFalse();
        assertThat(manager.claims(List.of("tps"))).isTrue();
        assertThat(manager.claims(List.of("fand:t"))).isFalse();
        assertThat(manager.claims(List.of("fand:tps"))).isTrue();
        assertThat(manager.claims(List.of("fand:config", "missing"))).isTrue();
    }

    @Test
    void rejectsInvalidNames() {
        var manager = new CommandManager(new PermissionManager());
        assertThatThrownBy(() -> manager.register("reload", command -> command.namespace("Bad Ns").executes(context -> {})))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preservesTypedArgumentsDuringNormalization() {
        var manager = new CommandManager(new PermissionManager());
        manager.register("Hello", command -> command
                .namespace("FAND")
                .aliases("Alias")
                .literal("Reload", reload -> reload
                        .argument("Targets", Arguments.word().asOptional(), target -> target.executes(context -> {}))));

        var command = manager.lookup("hello").orElseThrow();
        assertThat(command.arguments()).hasSize(1);
        var argument = command.arguments().getFirst();
        assertThat(argument.name()).isEqualTo("targets");
        assertThat(argument.optional()).isTrue();
        assertThat(argument.type()).isEqualTo(CommandArgumentType.WORD);
    }

    @Test
    void builderRegistersRootAndLiteralSubcommandsWithoutLocalRootConflict() throws Exception {
        var manager = new CommandManager(new PermissionManager());
        var executed = new ArrayList<String>();

        manager.register("minimotd", command -> command
                .namespace("demo")
                .permission("demo.admin")
                .executes(context -> executed.add("help:" + context.label()))
                .literal("about", about -> about.executes(context -> executed.add("about:" + context.label())))
                .literal("reload", reload -> reload.executes(context -> executed.add("reload:" + context.label()))));

        var sender = new TestSender("demo.admin");
        assertThat(manager.lookup("minimotd")).isPresent();
        assertThat(manager.resolve(sender, List.of("minimotd"))).isPresent();
        assertThat(manager.resolve(sender, List.of("minimotd", "reload"))).isPresent();
        assertThat(manager.suggestions(sender, List.of("minimotd", "r"))).containsExactly("reload");

        var resolved = manager.resolve(sender, List.of("minimotd", "about")).orElseThrow();
        resolved.command().execute(sender, resolved.usedLabel(), List.of());

        assertThat(executed).containsExactly("about:minimotd");
    }

    @Test
    void builderParsesTypedArgumentsAndOptionalDefaults() throws Exception {
        var manager = new CommandManager(new PermissionManager());
        var executed = new ArrayList<String>();

        manager.register("give", command -> command
                .namespace("demo")
                .argument("item", Arguments.word(), item -> item
                        .argument("amount", Arguments.integer(1, 2304).asOptional(1), amount -> amount
                                .executes(context -> executed.add(context.string("item") + ":" + context.intValue("amount"))))));

        var sender = new TestSender();
        var withAmount = manager.resolve(sender, List.of("give", "stone", "64")).orElseThrow();
        withAmount.command().execute(sender, withAmount.usedLabel(), List.of("stone", "64"));

        var defaultAmount = manager.resolve(sender, List.of("give", "dirt")).orElseThrow();
        defaultAmount.command().execute(sender, defaultAmount.usedLabel(), List.of("dirt"));

        assertThat(executed).containsExactly("stone:64", "dirt:1");
    }

    @Test
    void builderStaticEntrypointsCreateDefinitions() throws Exception {
        var manager = new CommandManager(new PermissionManager());
        var executed = new ArrayList<String>();

        manager.register(CommandBuilder.command("tool")
                .namespace("demo")
                .literal("reload", reload -> reload.executes(context -> executed.add(context.label())))
                .build());
        manager.register(CommandBuilder.define("echo", command -> command
                .namespace("demo")
                .argument("value", Arguments.word(), value -> value
                        .executes(context -> executed.add(context.string("value"))))));

        var sender = new TestSender();
        var reload = manager.resolve(sender, List.of("tool", "reload")).orElseThrow();
        reload.command().execute(sender, reload.usedLabel(), List.of());
        var echo = manager.resolve(sender, List.of("echo", "hello")).orElseThrow();
        echo.command().execute(sender, echo.usedLabel(), List.of("hello"));

        assertThat(executed).containsExactly("tool", "hello");
    }

    @Test
    void commandContextContainsReportsParsedArguments() {
        var context = new CommandContext(
                new TestSender(),
                "demo",
                List.of("stone"),
                Map.of("item", "stone"));

        assertThat(context.contains("item")).isTrue();
        assertThat(context.contains("amount")).isFalse();
    }

    @Test
    void builderRejectsDuplicateArgumentNameWithDifferentDefinition() {
        var builder = new io.fand.api.command.CommandBuilder("demo");
        builder.argument("value", Arguments.word());

        assertThatThrownBy(() -> builder.argument("value", Arguments.integer()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different definition");
    }

    @Test
    void builderAllowsReusingSameArgumentNodeForBranches() {
        var definition = new io.fand.api.command.CommandBuilder("demo")
                .argument("value", Arguments.word(), branch -> branch.executes(context -> {}))
                .argument("value", Arguments.word(), branch -> branch.literal("confirm", confirm -> confirm.executes(context -> {})))
                .build();

        assertThat(definition.root().children()).hasSize(1);
        assertThat(definition.root().children().getFirst().children()).hasSize(1);
    }

    @Test
    void commandInfoUsageOmitsBlankNamespacePrefix() {
        var local = new io.fand.api.command.CommandInfo("", "demo", List.of("reload"), List.of(), List.of(), null);
        var namespaced = new io.fand.api.command.CommandInfo("fand", "demo", List.of("reload"), List.of(), List.of(), null);

        assertThat(local.usage()).isEqualTo("demo reload");
        assertThat(namespaced.usage()).isEqualTo("fand:demo reload");
    }

    private static final class TestSender implements CommandSender, PermissionSubject {

        private final PermissionSet permissions;

        private TestSender(String... permissions) {
            this.permissions = new PermissionSet(false);
            for (var permission : permissions) {
                this.permissions.set(permission, true);
            }
        }

        @Override
        public String name() {
            return "test";
        }

        @Override
        public void sendMessage(Component message) {
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
