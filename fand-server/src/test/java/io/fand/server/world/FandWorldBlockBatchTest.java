package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class FandWorldBlockBatchTest {

    @Test
    void volumeCapsBeforeLongOverflow() {
        assertThat(FandWorld.volume(
                        Integer.MIN_VALUE,
                        0,
                        Integer.MIN_VALUE,
                        Integer.MAX_VALUE,
                        0,
                        Integer.MAX_VALUE))
                .isEqualTo((long) Integer.MAX_VALUE + 1L);
    }
}
