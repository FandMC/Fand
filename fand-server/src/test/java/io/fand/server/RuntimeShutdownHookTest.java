package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.internal.FandRuntime;
import io.fand.api.lifecycle.LifecyclePhase;
import io.fand.api.lifecycle.ServerStoppingEvent;
import io.fand.server.config.FandConfig;
import io.fand.server.hooks.FandHooks;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class RuntimeShutdownHookTest {

    @Test
    void closeUnbindsBothRuntimeAccessorsAndHooksFailFast() {
        var server = new FandServer(new FandConfig(), getClass().getClassLoader());
        Main.bind(server);
        FandRuntime.bind(server);
        try {
            server.close();

            assertThat(server.phase()).isEqualTo(LifecyclePhase.STOPPED);
            assertThatThrownBy(Main::runtime)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not been bootstrapped");
            assertThatThrownBy(FandRuntime::server)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not been bootstrapped");
            assertThatThrownBy(FandHooks::events).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(FandHooks::eventStructureVersion).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(FandHooks::performance).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(FandHooks::guiThemes).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(FandHooks::customBlocks).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(FandHooks::customItems).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> FandHooks.recordTickPerformance(1L, 1L, 0L))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            Main.unbind(server);
            FandRuntime.unbind(server);
        }
    }

    @Test
    void shutdownReasonIsDeliveredBeforeRuntimeStops() throws ReflectiveOperationException {
        var server = new FandServer(new FandConfig(), getClass().getClassLoader());
        var observedReason = new AtomicReference<String>();
        var observedThread = new AtomicReference<Thread>();
        setPhase(server, LifecyclePhase.RUNNING);
        server.events().subscribe(ServerStoppingEvent.class, event -> {
            observedReason.set(event.reason());
            observedThread.set(Thread.currentThread());
        });
        Main.bind(server);
        FandRuntime.bind(server);
        try {
            server.shutdown("test shutdown");

            assertThat(observedReason).hasValue("test shutdown");
            assertThat(observedThread).hasValue(Thread.currentThread());
            assertThat(server.phase()).isEqualTo(LifecyclePhase.STOPPED);
        } finally {
            server.close();
            Main.unbind(server);
            FandRuntime.unbind(server);
        }
    }

    @Test
    void fatalStoppingListenerDoesNotSkipRuntimeCleanup() throws ReflectiveOperationException {
        var server = new FandServer(new FandConfig(), getClass().getClassLoader());
        setPhase(server, LifecyclePhase.RUNNING);
        server.events().subscribe(ServerStoppingEvent.class, event -> {
            throw new AssertionError("fatal listener failure");
        });
        Main.bind(server);
        FandRuntime.bind(server);
        try {
            assertThatNoException().isThrownBy(server::close);

            assertThat(server.phase()).isEqualTo(LifecyclePhase.STOPPED);
            assertThatThrownBy(Main::runtime).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(FandRuntime::server).isInstanceOf(IllegalStateException.class);
        } finally {
            server.close();
            Main.unbind(server);
            FandRuntime.unbind(server);
        }
    }

    @Test
    void enableDoesNotOverwriteAnExistingShutdownRequest() throws ReflectiveOperationException {
        var server = new FandServer(new FandConfig(), getClass().getClassLoader());
        setPhase(server, LifecyclePhase.LOADED);
        setShutdownRequested(server);

        assertThatThrownBy(server::enable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Fand shutdown was requested during startup");
        assertThat(server.phase()).isEqualTo(LifecyclePhase.STOPPED);
    }

    @SuppressWarnings("unchecked")
    private static void setPhase(FandServer server, LifecyclePhase phase) throws ReflectiveOperationException {
        Field field = FandServer.class.getDeclaredField("phase");
        field.setAccessible(true);
        ((AtomicReference<LifecyclePhase>) field.get(server)).set(phase);
    }

    private static void setShutdownRequested(FandServer server) throws ReflectiveOperationException {
        Field field = FandServer.class.getDeclaredField("shutdownRequested");
        field.setAccessible(true);
        field.setBoolean(server, true);
    }
}
