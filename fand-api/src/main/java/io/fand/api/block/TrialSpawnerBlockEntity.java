package io.fand.api.block;

/** Trial Spawner lifecycle state. */
public interface TrialSpawnerBlockEntity extends BlockEntity {
    enum State { INACTIVE, WAITING_FOR_PLAYERS, ACTIVE, WAITING_FOR_REWARD_EJECTION, EJECTING_REWARD, COOLDOWN }
    State state();
    void setState(State state);
}
