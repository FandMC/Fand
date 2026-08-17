package io.fand.server.block;

import io.fand.api.block.CopperGolemStatueBlockEntity;
import io.fand.api.entity.CopperGolem;
import io.fand.server.hooks.FandHooks;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CopperGolemStatueBlock;

public final class FandCopperGolemStatueBlockEntity extends FandBlockEntity implements CopperGolemStatueBlockEntity {
    public FandCopperGolemStatueBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity handle) { super(block, handle); }
    @Override public net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity handle() { return (net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity) super.handle(); }
    @Override
    public Pose pose() {
        return block.callOnServerThread(() ->
                Pose.valueOf(handle().getBlockState().getValue(CopperGolemStatueBlock.POSE).name()));
    }

    @Override
    public void setPose(Pose pose) {
        Objects.requireNonNull(pose, "pose");
        block.runOnServerThread(() -> {
            var state = handle().getBlockState().setValue(
                    CopperGolemStatueBlock.POSE,
                    CopperGolemStatueBlock.Pose.valueOf(pose.name()));
            block.worldHandle().setBlock(block.position(), state, Block.UPDATE_ALL);
        });
    }

    @Override
    public Optional<? extends CopperGolem> awaken() {
        return block.callOnServerThread(() -> {
            var state = handle().getBlockState();
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.WeatheringCopperGolemStatueBlock weathering)
                    || weathering.getAge() != net.minecraft.world.level.block.WeatheringCopper.WeatherState.UNAFFECTED) {
                return Optional.empty();
            }
            var golem = handle().removeStatue(state);
            if (golem == null || !block.worldHandle().addFreshEntity(golem)) {
                return Optional.empty();
            }
            if (!block.worldHandle().removeBlock(block.position(), false)) {
                golem.discard();
                return Optional.empty();
            }
            var wrapped = FandHooks.wrapEntity(golem);
            return wrapped instanceof CopperGolem copperGolem
                    ? Optional.of(copperGolem)
                    : Optional.empty();
        });
    }
}
