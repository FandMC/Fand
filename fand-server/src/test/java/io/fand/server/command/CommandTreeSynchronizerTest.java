package io.fand.server.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class CommandTreeSynchronizerTest {

    @Test
    void coalescesRegistrationsAndUnregistrationsAndSkipsUnchangedTicks() {
        var commands = new CommandManager();
        var synchronizer = new CommandTreeSynchronizer(commands);
        var refreshes = new AtomicInteger();
        synchronizer.tick(refreshes::incrementAndGet);
        assertThat(refreshes.get()).isZero();

        var old = commands.register("demo", command -> command.namespace("test")
                .executes(context -> {})
                .literal("old", child -> child.executes(context -> {})));
        synchronizer.tick(refreshes::incrementAndGet);
        assertThat(refreshes.get()).isEqualTo(1);

        old.unregister();
        var replacement = commands.register("demo", command -> command.namespace("test")
                .literal("new", child -> child.executes(context -> {})));
        commands.register("other", command -> command.namespace("test").executes(context -> {}));
        synchronizer.tick(refreshes::incrementAndGet);
        synchronizer.tick(refreshes::incrementAndGet);
        assertThat(refreshes.get()).isEqualTo(2);

        replacement.unregister();
        synchronizer.tick(refreshes::incrementAndGet);
        replacement.unregister();
        synchronizer.tick(refreshes::incrementAndGet);
        assertThat(refreshes.get()).isEqualTo(3);
    }

    @Test
    void publishesOneRevisionForWholeCommandRemoval() {
        var commands = new CommandManager();
        var registration = commands.register("demo", command -> command.namespace("test")
                .executes(context -> {})
                .literal("first", child -> child.executes(context -> {}))
                .literal("second", child -> child.executes(context -> {})));
        long before = commands.revision();

        registration.unregister();

        assertThat(commands.revision()).isEqualTo(before + 1);
        assertThat(registration.active()).isFalse();
        registration.unregister();
        assertThat(commands.revision()).isEqualTo(before + 1);
    }

    @Test
    void keepsChangesDuringRefreshPending() {
        var commands = new CommandManager();
        var synchronizer = new CommandTreeSynchronizer(commands);
        var refreshes = new AtomicInteger();
        commands.register("demo", command -> command.namespace("test").executes(context -> {}));

        synchronizer.tick(() -> commands.register("next", command -> command.namespace("test").executes(context -> {})));
        synchronizer.tick(refreshes::incrementAndGet);
        synchronizer.tick(refreshes::incrementAndGet);

        assertThat(refreshes.get()).isEqualTo(1);
    }

    @Test
    void retriesFailedRefreshAndDoesNotRefreshRejectedRegistration() {
        var commands = new CommandManager();
        var synchronizer = new CommandTreeSynchronizer(commands);
        var refreshes = new AtomicInteger();
        commands.register("demo", command -> command.namespace("test").executes(context -> {}));

        assertThatThrownBy(() -> synchronizer.tick(() -> {
            throw new IllegalStateException("connection failure");
        })).isInstanceOf(IllegalStateException.class);
        synchronizer.tick(refreshes::incrementAndGet);
        assertThat(refreshes.get()).isEqualTo(1);

        assertThatThrownBy(() -> commands.register("demo", command -> command.namespace("test").executes(context -> {})))
                .isInstanceOf(IllegalStateException.class);
        synchronizer.tick(refreshes::incrementAndGet);
        assertThat(refreshes.get()).isEqualTo(1);
    }
}
