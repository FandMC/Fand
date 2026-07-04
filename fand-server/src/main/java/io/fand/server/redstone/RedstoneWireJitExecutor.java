package io.fand.server.redstone;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Redstone;

public final class RedstoneWireJitExecutor {

    private static final int MAX_PLANS = 65_536;
    private static final int WARMUP_ATTEMPTS = 3;
    private static final long RECOMPILE_COOLDOWN_ATTEMPTS = 8_192L;
    private static final int MAX_COOLDOWN_SHIFT = 5;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final ConcurrentHashMap<Level, LevelPlans> levels = new ConcurrentHashMap<>();
    private long attempts;
    private long hits;
    private long compiled;
    private long guardMisses;
    private long capacityMisses;
    private long warmupMisses;
    private long cooldownMisses;
    private long invalidations;
    private long blockInvalidations;
    private long chunkInvalidations;

    public boolean tryUpdatePowerStrength(
            RedStoneWireBlock wireBlock,
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        attempts++;
        if (level.isClientSide() || !state.is(wireBlock) || level.getBlockState(pos) != state) {
            return false;
        }

        var levelPlans = levels.computeIfAbsent(level, ignored -> new LevelPlans());
        long packedPos = pos.asLong();
        var plan = levelPlans.plans.get(packedPos);
        if (plan == null) {
            if (levelPlans.isCoolingDown(packedPos, attempts)) {
                cooldownMisses++;
                return false;
            }
            if (!levelPlans.warmup(packedPos)) {
                warmupMisses++;
                return false;
            }
            plan = compilePlan(levelPlans, packedPos, level, pos, wireBlock);
            if (plan == null) {
                return false;
            }
        }

        if (!plan.valid(level)) {
            guardMisses++;
            levelPlans.remove(packedPos, plan, attempts);
            return false;
        }

        if (!plan.execute(level, wireBlock, state)) {
            levelPlans.remove(packedPos, plan, attempts);
            return false;
        }

        hits++;
        return true;
    }

    public RedstoneWireJitSnapshot snapshot() {
        return new RedstoneWireJitSnapshot(
                planCount(),
                attempts,
                hits,
                compiled,
                guardMisses,
                capacityMisses,
                warmupMisses,
                cooldownMisses,
                invalidations,
                blockInvalidations,
                chunkInvalidations);
    }

    public void invalidateBlock(Level level, long blockPos) {
        var levelPlans = levels.get(level);
        if (levelPlans == null) {
            return;
        }
        int x = BlockPos.getX(blockPos);
        int y = BlockPos.getY(blockPos);
        int z = BlockPos.getZ(blockPos);
        invalidateBlockPlan(levelPlans, x, y, z);
        invalidateBlockPlan(levelPlans, x, y - 1, z);
        invalidateBlockPlan(levelPlans, x + 1, y, z);
        invalidateBlockPlan(levelPlans, x - 1, y, z);
        invalidateBlockPlan(levelPlans, x, y, z + 1);
        invalidateBlockPlan(levelPlans, x, y, z - 1);
    }

    public void invalidateChunk(Level level, int chunkX, int chunkZ) {
        var levelPlans = levels.get(level);
        if (levelPlans == null) {
            return;
        }
        var iterator = levelPlans.plans.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            long packedPos = entry.getLongKey();
            int planChunkX = BlockPos.getX(packedPos) >> 4;
            int planChunkZ = BlockPos.getZ(packedPos) >> 4;
            if (Math.abs(planChunkX - chunkX) <= 1 && Math.abs(planChunkZ - chunkZ) <= 1) {
                iterator.remove();
                levelPlans.onInvalidated(packedPos, attempts);
                invalidations++;
                chunkInvalidations++;
            }
        }
    }

    private void invalidateBlockPlan(LevelPlans levelPlans, int x, int y, int z) {
        long packedPos = BlockPos.asLong(x, y, z);
        if (levelPlans.remove(packedPos) != null) {
            invalidations++;
            blockInvalidations++;
            levelPlans.onInvalidated(packedPos, attempts);
        }
    }

    public void clear() {
        levels.clear();
        attempts = 0L;
        hits = 0L;
        compiled = 0L;
        guardMisses = 0L;
        capacityMisses = 0L;
        warmupMisses = 0L;
        cooldownMisses = 0L;
        invalidations = 0L;
        blockInvalidations = 0L;
        chunkInvalidations = 0L;
    }

    private WirePlan compilePlan(LevelPlans levelPlans, long packedPos, Level level, BlockPos pos, RedStoneWireBlock wireBlock) {
        if (levelPlans.plans.size() >= MAX_PLANS) {
            capacityMisses++;
            return null;
        }

        var compiledPlan = WirePlan.compile(level, pos);
        levelPlans.put(packedPos, compiledPlan);
        compiled++;
        return compiledPlan;
    }

    private int planCount() {
        int count = 0;
        for (var levelPlans : levels.values()) {
            count += levelPlans.plans.size();
        }
        return count;
    }

    private static final class LevelPlans {
        private final Long2ObjectOpenHashMap<WirePlan> plans = new Long2ObjectOpenHashMap<>();
        private final Long2ByteOpenHashMap warmups = new Long2ByteOpenHashMap();
        private final Long2LongOpenHashMap cooldowns = new Long2LongOpenHashMap();
        private final Long2ByteOpenHashMap cooldownShifts = new Long2ByteOpenHashMap();

        private LevelPlans() {
            this.cooldowns.defaultReturnValue(Long.MIN_VALUE);
        }

        private boolean warmup(long packedPos) {
            byte count = this.warmups.addTo(packedPos, (byte)1);
            return count + 1 >= WARMUP_ATTEMPTS;
        }

        private boolean isCoolingDown(long packedPos, long attempt) {
            long cooldownUntil = this.cooldowns.get(packedPos);
            if (cooldownUntil == Long.MIN_VALUE) {
                return false;
            }
            if (attempt < cooldownUntil) {
                return true;
            }
            this.cooldowns.remove(packedPos);
            return false;
        }

        private void put(long packedPos, WirePlan plan) {
            this.plans.put(packedPos, plan);
            this.warmups.remove(packedPos);
        }

        private WirePlan remove(long packedPos) {
            var plan = this.plans.remove(packedPos);
            if (plan == null) {
                this.warmups.remove(packedPos);
            }
            return plan;
        }

        private boolean remove(long packedPos, WirePlan expected, long attempt) {
            var plan = this.plans.get(packedPos);
            if (plan != expected) {
                return false;
            }
            this.plans.remove(packedPos);
            this.onInvalidated(packedPos, attempt);
            return true;
        }

        private void onInvalidated(long packedPos, long attempt) {
            this.warmups.remove(packedPos);
            int shift = Math.min(MAX_COOLDOWN_SHIFT, this.cooldownShifts.get(packedPos) + 1);
            this.cooldownShifts.put(packedPos, (byte)shift);
            this.cooldowns.put(packedPos, attempt + (RECOMPILE_COOLDOWN_ATTEMPTS << shift));
        }
    }

    private record WirePlan(
            BlockPos pos,
            BlockPos abovePos,
            boolean aboveConductor,
            NeighborGuard[] neighborGuards,
            BlockPos[] signalPositions,
            Direction[] signalDirections,
            BlockPos[] inputPositions,
            BlockPos[] updateOrder
    ) {

        private static WirePlan compile(Level level, BlockPos pos) {
            var inputs = new ArrayList<BlockPos>(12);
            var guards = new NeighborGuard[4];
            BlockPos above = pos.above();
            boolean aboveConductor = level.getBlockState(above).isRedstoneConductor(level, above);
            int index = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighbor);
                boolean neighborConductor = neighborState.isRedstoneConductor(level, neighbor);
                guards[index++] = new NeighborGuard(neighbor, neighborConductor);
                inputs.add(neighbor);
                if (neighborConductor) {
                    if (!aboveConductor) {
                        inputs.add(neighbor.above());
                    }
                } else {
                    inputs.add(neighbor.below());
                }
            }

            BlockPos[] signalPositions = new BlockPos[DIRECTIONS.length];
            Direction[] signalDirections = new Direction[DIRECTIONS.length];
            for (int signalIndex = 0; signalIndex < DIRECTIONS.length; signalIndex++) {
                Direction direction = DIRECTIONS[signalIndex];
                signalPositions[signalIndex] = pos.relative(direction);
                signalDirections[signalIndex] = direction;
            }

            return new WirePlan(
                    pos,
                    above,
                    aboveConductor,
                    guards,
                    signalPositions,
                    signalDirections,
                    inputs.toArray(BlockPos[]::new),
                    updateOrder(pos));
        }

        private static BlockPos[] updateOrder(BlockPos pos) {
            return new BlockPos[] {pos, pos.below(), pos.above(), pos.north(), pos.south(), pos.west(), pos.east()};
        }

        private boolean valid(Level level) {
            if (level.getBlockState(abovePos).isRedstoneConductor(level, abovePos) != aboveConductor) {
                return false;
            }
            for (var guard : neighborGuards) {
                if (level.getBlockState(guard.pos()).isRedstoneConductor(level, guard.pos()) != guard.conductor()) {
                    return false;
                }
            }
            return true;
        }

        private boolean execute(Level level, RedStoneWireBlock wireBlock, BlockState state) {
            int blockSignal = wireBlock.fand$getBlockSignal(level, signalPositions, signalDirections);
            int targetStrength = blockSignal == Redstone.SIGNAL_MAX
                    ? blockSignal
                    : Math.max(blockSignal, incomingWireSignal(level, wireBlock));
            int oldStrength = state.getValue(RedStoneWireBlock.POWER);
            if (level instanceof ServerLevel serverLevel) {
                targetStrength = io.fand.server.event.BlockEvents.fireRedstone(serverLevel, pos, oldStrength, targetStrength);
            }
            if (oldStrength == targetStrength) {
                return true;
            }

            if (level.getBlockState(pos) != state) {
                return false;
            }
            level.setBlock(pos, state.setValue(RedStoneWireBlock.POWER, targetStrength), Block.UPDATE_CLIENTS);
            for (var updatePos : updateOrder) {
                level.updateNeighborsAt(updatePos, wireBlock);
            }
            return true;
        }

        private int incomingWireSignal(Level level, RedStoneWireBlock wireBlock) {
            int signal = 0;
            for (var inputPos : inputPositions) {
                BlockState inputState = level.getBlockState(inputPos);
                if (inputState.is(wireBlock)) {
                    signal = Math.max(signal, inputState.getValue(RedStoneWireBlock.POWER));
                }
            }
            return Math.max(0, signal - 1);
        }
    }

    private record NeighborGuard(BlockPos pos, boolean conductor) {
    }
}
