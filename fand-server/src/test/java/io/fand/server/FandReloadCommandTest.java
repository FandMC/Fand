package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.command.CommandSender;
import io.fand.api.permission.PermissionSubject;
import io.fand.server.config.FandConfig;
import io.fand.server.console.gui.GuiTheme;
import io.fand.server.permission.PermissionSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FandReloadCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void registersReloadCommandWithPermissionAndExecutesReload() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                identity:
                  brand: Fand

                plugins:
                  directory: plugins
                  continueOnLoadFailure: false
                  continueOnEnableFailure: false
                  logSummary: true

                scheduler:
                  asyncThreads: 0
                """);

        var server = new FandServer(path, FandConfig.load(path), getClass().getClassLoader());
        var denied = new TestSender(false);
        var allowed = new TestSender(true);

        assertThat(server.commandManager().resolve(denied, List.of("fand", "reload"))).isEmpty();
        assertThat(server.commandManager().suggestions(denied, List.of("fand", "r"))).isEmpty();
        assertThat(server.commandManager().suggestions(allowed, List.of("fand", "r"))).containsExactly("reload");

        Files.writeString(path, """
                identity:
                  brand: 'Reloaded Brand'

                plugins:
                  directory: plugins
                  continueOnLoadFailure: true
                  continueOnEnableFailure: false
                  logSummary: true

                scheduler:
                  asyncThreads: 0

                console:
                  gui:
                    enabled: false
                    theme: dark
                """);

        var resolved = server.commandManager().resolve(allowed, List.of("fand", "reload")).orElseThrow();
        resolved.command().execute(allowed, resolved.usedLabel(), List.of());

        assertThat(server.brand()).isEqualTo("Reloaded Brand");
        assertThat(server.guiThemes().current())
                .isEqualTo(io.fand.server.console.gui.GuiTheme.DARK);
        assertThat(allowed.messages).containsExactly(
                Component.text("Hot-applied: identity.brand, plugins.continueOnLoadFailure, console.gui.theme"),
                Component.text("Requires restart: console.gui.enabled")
        );
    }

    @Test
    void guiThemeSelectionPersistsAcrossServerRestart() throws Exception {
        var path = tempDir.resolve("fand.yml");
        FandConfig.load(path);

        try (var server = new FandServer(path, FandConfig.load(path), getClass().getClassLoader())) {
            server.guiThemes().select(GuiTheme.DARK);
        }

        assertThat(FandConfig.load(path).console.gui.theme).isEqualTo("dark");

        try (var restarted = new FandServer(path, FandConfig.load(path), getClass().getClassLoader())) {
            assertThat(restarted.guiThemes().current()).isEqualTo(GuiTheme.DARK);
        }
    }

    private static final class TestSender implements CommandSender, PermissionSubject {

        private final PermissionSet permissions;
        private final List<Component> messages = new ArrayList<>();

        private TestSender(boolean allowReload) {
            this.permissions = new PermissionSet(false);
            if (allowReload) {
                this.permissions.set("fand.command.reload", true);
            }
        }

        @Override
        public String name() {
            return "test";
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
