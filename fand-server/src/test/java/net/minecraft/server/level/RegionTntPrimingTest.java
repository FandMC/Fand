package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegionTntPrimingTest {
    private static final BlockPos LEFT = new BlockPos(8, 64, 8);
    private static final BlockPos RIGHT = new BlockPos(264, 64, 8);

    @BeforeAll
    static void bootstrap() {
        RegionBlockEntityTickerTest.bootstrap();
    }

    @Test
    void nativePrimingOverlapsButPublishesInRegionOrderAfterBothRegionsFinish() {
        try (Fixture fixture = new Fixture(2)) {
            var entered = new CountDownLatch(2);
            fixture.requiredFinished = 2;
            fixture.run(() -> {
                awaitBoth(entered);
                BlockPos.MutableBlockPos pos = LEFT.mutable();
                assertThat(TntBlock.prime(fixture.base.level, pos)).isTrue();
                pos.set(RIGHT);
                fixture.neighborChanged(LEFT.east());
                assertThat(fixture.constructed).hasValue(0);
                assertThat(fixture.effects).isEmpty();
                fixture.finished.incrementAndGet();
            }, () -> {
                awaitBoth(entered);
                fixture.neighborChanged(RIGHT);
                assertThat(fixture.constructed).hasValue(0);
                assertThat(fixture.effects).isEmpty();
                fixture.finished.incrementAndGet();
            });
            assertThat(fixture.base.runner.snapshot().peakConcurrentRegions()).isEqualTo(2);
            assertThat(fixture.removed).hasValue(2);
            assertThat(fixture.spawned).extracting(PrimedTnt::blockPosition).containsExactly(LEFT, LEFT.east(), RIGHT);
            assertThat(fixture.spawned).allSatisfy(entity -> assertThat(entity.getFuse()).isEqualTo(80));
            assertThat(fixture.effects).containsExactly(
                new Effect("spawn", LEFT), new Effect("sound", LEFT), new Effect("event", LEFT),
                new Effect("spawn", LEFT.east()), new Effect("sound", LEFT.east()), new Effect("event", LEFT.east()),
                new Effect("spawn", RIGHT), new Effect("sound", RIGHT), new Effect("event", RIGHT));
        }
    }

    @Test
    void disabledGameRuleDoesNotAcceptPrimingOrQueueEffects() {
        try (Fixture fixture = new Fixture(2)) {
            fixture.enabled.set(false);
            fixture.run(() -> {
                assertThat(TntBlock.prime(fixture.base.level, LEFT)).isFalse();
                fixture.neighborChanged(LEFT);
            }, () -> {
                assertThat(TntBlock.prime(fixture.base.level, RIGHT)).isFalse();
                fixture.neighborChanged(RIGHT);
            });
            assertThat(fixture.constructed).hasValue(0);
            assertThat(fixture.removed).hasValue(0);
            assertThat(fixture.effects).isEmpty();
        }
    }

    @Test
    void acceptedPrimingDoesNotRecheckTheGameRuleDuringPublication() {
        try (Fixture fixture = new Fixture(2)) {
            fixture.run(() -> {
                RegionTickScope.deferAt(fixture.base.level, LEFT, () -> fixture.enabled.set(false));
                assertThat(TntBlock.prime(fixture.base.level, LEFT)).isTrue();
            }, () -> assertThat(TntBlock.prime(fixture.base.level, RIGHT)).isTrue());
            assertThat(fixture.enabled).isFalse();
            assertThat(fixture.spawned).extracting(PrimedTnt::blockPosition).containsExactly(LEFT, RIGHT);
            verify(fixture.rules, times(2)).get(GameRules.TNT_EXPLODES);
        }
    }

    @Test
    void crossRegionPrimingIsRejectedBeforeAnyEntityOrEffectIsQueued() {
        try (Fixture fixture = new Fixture(2)) {
            fixture.run(() -> assertThatThrownBy(() -> TntBlock.prime(fixture.base.level, RIGHT))
                .isInstanceOf(IllegalStateException.class), () -> {});
            assertThat(fixture.constructed).hasValue(0);
            assertThat(fixture.effects).isEmpty();
        }
    }

    @Test
    void refusedRegistrationStillConsumesTheBlockAndPublishesItsPrimingEffects() {
        try (Fixture fixture = new Fixture(2)) {
            fixture.registrationAccepted = false;
            fixture.run(() -> fixture.neighborChanged(LEFT), () -> fixture.neighborChanged(RIGHT));
            assertThat(fixture.removed).hasValue(2);
            assertThat(fixture.effects).containsExactly(
                new Effect("spawn", LEFT), new Effect("sound", LEFT), new Effect("event", LEFT),
                new Effect("spawn", RIGHT), new Effect("sound", RIGHT), new Effect("event", RIGHT));
        }
    }

    @Test
    void controlThreadPrimingRetainsSynchronousReturnAndEffects() {
        try (Fixture fixture = new Fixture(1)) {
            fixture.registrationAccepted = false;
            assertThat(TntBlock.prime(fixture.base.level, LEFT)).isTrue();
            assertThat(fixture.constructed).hasValue(1);
            assertThat(fixture.effects).containsExactly(
                new Effect("spawn", LEFT), new Effect("sound", LEFT), new Effect("event", LEFT));
        }
    }

    private static void awaitBoth(CountDownLatch entered) {
        entered.countDown();
        try {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
    }

    private record Effect(String kind, BlockPos pos) {}

    private record Ticker(BlockPos getPos, Runnable action) implements TickingBlockEntity {
        @Override public void tick() { this.action.run(); }
        @Override public boolean isRemoved() { return false; }
        @Override public boolean fand$canTickInRegion() { return true; }
        @Override public String getType() { return "test:tnt_priming"; }
    }

    private static final class Fixture implements AutoCloseable {
        final RegionBlockEntityTickerTest.Fixture base;
        final GameRules rules = mock(GameRules.class);
        final AtomicBoolean enabled = new AtomicBoolean(true);
        final AtomicInteger constructed = new AtomicInteger();
        final AtomicInteger removed = new AtomicInteger();
        final AtomicInteger finished = new AtomicInteger();
        final List<PrimedTnt> spawned = new ArrayList<>();
        final List<Effect> effects = new ArrayList<>();
        final Thread control = Thread.currentThread();
        boolean registrationAccepted = true;
        int requiredFinished;

        Fixture(int workers) {
            this.base = new RegionBlockEntityTickerTest.Fixture(workers);
            when(this.base.level.getGameRules()).thenReturn(this.rules);
            when(this.rules.get(GameRules.TNT_EXPLODES)).thenAnswer(call -> this.enabled.get());
            when(this.base.level.hasNeighborSignal(any())).thenReturn(true);
            when(this.base.level.getNextEntityId()).thenAnswer(call -> {
                this.requirePublication();
                return this.constructed.incrementAndGet();
            });
            when(this.base.level.removeBlock(any(), eq(false))).thenAnswer(call -> {
                var scope = RegionTickScope.current();
                if (scope != null) scope.requirePosition(this.base.level, call.getArgument(0));
                this.removed.incrementAndGet();
                return true;
            });
            when(this.base.level.addFreshEntity(any())).thenAnswer(call -> {
                this.requirePublication();
                PrimedTnt entity = call.getArgument(0);
                this.spawned.add(entity);
                this.effects.add(new Effect("spawn", entity.blockPosition()));
                return this.registrationAccepted;
            });
            doAnswer(call -> {
                this.requirePublication();
                this.effects.add(new Effect("sound", BlockPos.containing(
                    call.<Double>getArgument(1), call.<Double>getArgument(2), call.<Double>getArgument(3))));
                return null;
            }).when(this.base.level).playSound(isNull(), anyDouble(), anyDouble(), anyDouble(),
                eq(SoundEvents.TNT_PRIMED), eq(SoundSource.BLOCKS), eq(1.0F), eq(1.0F));
            doAnswer(call -> {
                this.requirePublication();
                this.effects.add(new Effect("event", call.getArgument(2)));
                return null;
            }).when(this.base.level).gameEvent(isNull(), eq(GameEvent.PRIME_FUSE), any(BlockPos.class));
        }

        void requirePublication() {
            assertThat(Thread.currentThread()).isSameAs(this.control);
            assertThat(RegionTickScope.current()).isNull();
            assertThat(this.finished.get()).isEqualTo(this.requiredFinished);
        }

        void neighborChanged(BlockPos pos) {
            int before = this.removed.get();
            Blocks.TNT.defaultBlockState().handleNeighborChanged(this.base.level, pos, Blocks.REDSTONE_BLOCK, null, false);
            if (this.enabled.get()) assertThat(this.removed.get()).isGreaterThan(before);
        }

        void run(Runnable left, Runnable right) {
            this.base.runner.tick(List.of(new Ticker(LEFT, left), new Ticker(RIGHT, right)), TickingBlockEntity::tick);
        }

        @Override public void close() { this.base.close(); }
    }
}
