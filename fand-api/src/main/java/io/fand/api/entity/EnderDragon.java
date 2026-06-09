package io.fand.api.entity;

/** Ender dragon-specific fight and phase controls. */
public interface EnderDragon extends Mob {

    Phase phase();

    void setPhase(Phase phase);

    boolean sitting();

    boolean inDragonFight();

    int aliveCrystals();

    boolean previouslyKilled();

    enum Phase {
        HOLDING_PATTERN,
        STRAFE_PLAYER,
        LANDING_APPROACH,
        LANDING,
        TAKEOFF,
        SITTING_FLAMING,
        SITTING_SCANNING,
        SITTING_ATTACKING,
        CHARGING_PLAYER,
        DYING,
        HOVERING
    }
}
