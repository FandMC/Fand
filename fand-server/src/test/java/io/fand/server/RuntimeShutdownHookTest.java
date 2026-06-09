package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.fand.api.event.Event;
import io.fand.api.lifecycle.LifecyclePhase;
import io.fand.server.config.FandConfig;
import io.fand.server.hooks.FandHooks;
import org.junit.jupiter.api.Test;

final class RuntimeShutdownHookTest {

    @Test
    void hooksNoopAfterRuntimeClosesButBeforeStaticUnbind() {
        var server = new FandServer(new FandConfig(), getClass().getClassLoader());
        Main.bind(server);
        try {
            server.close();

            assertThat(server.phase()).isEqualTo(LifecyclePhase.STOPPED);
            assertThat(FandHooks.hasListeners(RuntimeHookTestEvent.class)).isFalse();
            assertThat(FandHooks.events().fire(new RuntimeHookTestEvent())).isNotNull();
            assertThat(FandHooks.performance().tickCount()).isZero();
            assertThatNoException().isThrownBy(() -> FandHooks.recordTickPerformance(1L, 1L, 0L));
        } finally {
            Main.unbind(server);
        }
    }

    @Test
    void hooksNoopWhenRuntimeIsAlreadyUnbound() {
        assertThat(FandHooks.hasListeners(RuntimeHookTestEvent.class)).isFalse();
        assertThat(FandHooks.performance().tickCount()).isZero();
        assertThatNoException().isThrownBy(() -> FandHooks.recordTickPerformance(1L, 1L, 0L));
    }

    private record RuntimeHookTestEvent() implements Event {
    }
}
