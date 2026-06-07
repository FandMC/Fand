package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.command.CommandSender;
import io.fand.api.permission.PermissionSubject;
import io.fand.server.config.FandConfig;
import io.fand.server.permission.PermissionSet;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class PerformanceCommandsTest {

    @Test
    void registersTpsAndMsptCommandsWithOperatorPermission() {
        var server = new FandServer(new FandConfig(), getClass().getClassLoader());
        var denied = new TestSender(false);
        var allowed = new TestSender(true);

        assertThat(server.commandManager().resolve(denied, List.of("tps"))).isEmpty();
        assertThat(server.commandManager().resolve(denied, List.of("mspt"))).isEmpty();
        assertThat(server.commandManager().resolve(allowed, List.of("tps"))).isPresent();
        assertThat(server.commandManager().resolve(allowed, List.of("mspt"))).isPresent();
    }

    @Test
    void reportsPerformanceSnapshots() throws Exception {
        var server = new FandServer(new FandConfig(), getClass().getClassLoader());
        var sender = new TestSender(true);

        server.recordTick(0L, 25_000_000L);
        server.recordTick(50_000_000L, 75_000_000L);

        var tps = server.commandManager().resolve(sender, List.of("tps")).orElseThrow();
        tps.command().executor().execute(sender, tps.usedLabel(), List.of());
        var mspt = server.commandManager().resolve(sender, List.of("mspt")).orElseThrow();
        mspt.command().executor().execute(sender, mspt.usedLabel(), List.of());

        assertThat(sender.messages).containsExactly(
                Component.text("TPS: 20.00, 20.00, 20.00 (1m, 5m, 15m)"),
                Component.text("MSPT (avg/min/max): 50.00/25.00/75.00, 50.00/25.00/75.00, 50.00/25.00/75.00 (5s, 10s, 1m)")
        );
    }

    private static final class TestSender implements CommandSender, PermissionSubject {

        private final PermissionSet permissions;
        private final List<Component> messages = new ArrayList<>();

        private TestSender(boolean operator) {
            this.permissions = new PermissionSet(operator);
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
        public boolean hasPermission(String permission) {
            return permissions.operator();
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
