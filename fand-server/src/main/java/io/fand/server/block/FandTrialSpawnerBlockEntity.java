package io.fand.server.block;

import io.fand.api.block.TrialSpawnerBlockEntity;

public final class FandTrialSpawnerBlockEntity extends FandBlockEntity implements TrialSpawnerBlockEntity {
    public FandTrialSpawnerBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity handle) { super(block, handle); }
    @Override public net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity handle() { return (net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity) super.handle(); }
    @Override public State state() { return block.callOnServerThread(() -> State.valueOf(handle().getState().name())); }

    @Override
    public void setState(State state) {
        java.util.Objects.requireNonNull(state, "state");
        block.runOnServerThread(() -> handle().setState(
                block.worldHandle(),
                net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState.valueOf(state.name())));
    }
}
