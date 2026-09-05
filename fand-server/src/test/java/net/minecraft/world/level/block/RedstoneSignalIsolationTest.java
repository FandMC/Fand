package net.minecraft.world.level.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RedstoneSignalIsolationTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void oneRegionsQueryDoesNotDisableWireSignalsInAnother() throws Exception {
        RedStoneWireBlock wire = (RedStoneWireBlock)Blocks.REDSTONE_WIRE;
        Level level = mock(Level.class);
        CountDownLatch querying = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(level.getBestNeighborSignal(any())).thenAnswer(call -> {
            assertThat(wire.defaultBlockState().isSignalSource()).isFalse();
            querying.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return 11;
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(() -> wire.getBlockSignal(level, BlockPos.ZERO));
            try {
                assertThat(querying.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(wire.defaultBlockState().isSignalSource()).isTrue();
            } finally {
                release.countDown();
            }
            assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo(11);
        }
    }

    @Test
    void nestedQueriesAndFailuresRestoreTheOuterSignalState() {
        RedStoneWireBlock wire = (RedStoneWireBlock)Blocks.REDSTONE_WIRE;
        Level outer = mock(Level.class);
        Level nested = mock(Level.class);
        when(nested.getBestNeighborSignal(any())).thenThrow(new IllegalStateException("signal failure"));
        when(outer.getBestNeighborSignal(any())).thenAnswer(call -> {
            assertThatThrownBy(() -> wire.getBlockSignal(nested, BlockPos.ZERO)).hasMessage("signal failure");
            assertThat(wire.defaultBlockState().isSignalSource()).isFalse();
            return 7;
        });
        assertThat(wire.getBlockSignal(outer, BlockPos.ZERO)).isEqualTo(7);
        assertThat(wire.defaultBlockState().isSignalSource()).isTrue();
        assertThatThrownBy(() -> wire.getBlockSignal(nested, BlockPos.ZERO)).hasMessage("signal failure");
        assertThat(wire.defaultBlockState().isSignalSource()).isTrue();
    }
}
