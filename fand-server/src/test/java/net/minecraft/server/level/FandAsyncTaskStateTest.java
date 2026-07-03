package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class FandAsyncTaskStateTest {

    @Test
    void coalescesWakeupsWhileAlreadyQueued() {
        FandAsyncTaskState state = new FandAsyncTaskState();

        assertThat(state.markQueued()).isTrue();
        assertThat(state.markQueued()).isFalse();
    }

    @Test
    void recordsOneFollowupDispatchWhenWakeupArrivesWhileRunning() {
        FandAsyncTaskState state = new FandAsyncTaskState();

        assertThat(state.markQueued()).isTrue();
        assertThat(state.markDispatchStarted()).isTrue();
        assertThat(state.markQueued()).isFalse();
        assertThat(state.markQueued()).isFalse();

        assertThat(state.markDispatchFinished()).isTrue();
        assertThat(state.markQueued()).isTrue();
    }

    @Test
    void finishesCleanlyWithoutFollowupWhenNoWakeupArrived() {
        FandAsyncTaskState state = new FandAsyncTaskState();

        assertThat(state.markQueued()).isTrue();
        assertThat(state.markDispatchStarted()).isTrue();

        assertThat(state.markDispatchFinished()).isFalse();
        assertThat(state.markQueued()).isTrue();
    }
}
