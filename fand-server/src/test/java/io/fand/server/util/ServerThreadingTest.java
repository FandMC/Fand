package io.fand.server.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.MinecraftServer;
import org.junit.jupiter.api.Test;

final class ServerThreadingTest {

    @Test
    void missingServerNeverRunsTaskOnCallingThread() {
        var calls = new AtomicInteger();

        assertThat(ServerThreading.run(null, calls::incrementAndGet)).isFalse();
        assertThatThrownBy(() -> ServerThreading.runFuture(null, calls::incrementAndGet).join())
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ServerThreading.callFuture(null, calls::incrementAndGet).join())
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ServerThreading.callBlocking(null, calls::incrementAndGet))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Minecraft server is not attached");
        assertThat(calls).hasValue(0);
    }

    @Test
    void stoppedServerRejectsOffThreadTask() {
        var server = mock(MinecraftServer.class);
        var calls = new AtomicInteger();
        when(server.isSameThread()).thenReturn(false);
        when(server.isRunning()).thenReturn(false);

        assertThat(ServerThreading.run(server, calls::incrementAndGet)).isFalse();
        assertThatThrownBy(() -> ServerThreading.callFuture(server, calls::incrementAndGet).join())
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(calls).hasValue(0);
    }

    @Test
    void serverThreadCanFinishInlineShutdownWork() {
        var server = mock(MinecraftServer.class);
        var calls = new AtomicInteger();
        when(server.isSameThread()).thenReturn(true);
        when(server.isRunning()).thenReturn(false);

        assertThat(ServerThreading.run(server, calls::incrementAndGet)).isTrue();
        assertThat(ServerThreading.callBlocking(server, calls::incrementAndGet)).isEqualTo(2);
        assertThat(calls).hasValue(2);
    }
}
