package io.fand.server.entity;

import io.fand.api.entity.EnderDragon;
import io.fand.server.world.WorldRegistry;
import java.util.Objects;

public final class FandEnderDragon extends FandMob implements EnderDragon {

    public FandEnderDragon(net.minecraft.world.entity.boss.enderdragon.EnderDragon handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.boss.enderdragon.EnderDragon handle() {
        return (net.minecraft.world.entity.boss.enderdragon.EnderDragon) handle;
    }

    @Override
    public Phase phase() {
        return toApi(handle().getPhaseManager().getCurrentPhase().getPhase());
    }

    @Override
    public void setPhase(Phase phase) {
        Objects.requireNonNull(phase, "phase");
        runOnServerThread(() -> handle().getPhaseManager().setPhase(toVanilla(phase)));
    }

    @Override
    public boolean sitting() {
        return handle().getPhaseManager().getCurrentPhase().isSitting();
    }

    @Override
    public boolean inDragonFight() {
        return handle().getDragonFight() != null;
    }

    @Override
    public int aliveCrystals() {
        var fight = handle().getDragonFight();
        return fight == null ? 0 : fight.aliveCrystals();
    }

    @Override
    public boolean previouslyKilled() {
        var fight = handle().getDragonFight();
        return fight != null && fight.hasPreviouslyKilledDragon();
    }

    private static Phase toApi(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase<?> phase) {
        if (phase == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN) {
            return Phase.HOLDING_PATTERN;
        }
        if (phase == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.STRAFE_PLAYER) {
            return Phase.STRAFE_PLAYER;
        }
        if (phase == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.LANDING_APPROACH) {
            return Phase.LANDING_APPROACH;
        }
        if (phase == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.LANDING) {
            return Phase.LANDING;
        }
        if (phase == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.TAKEOFF) {
            return Phase.TAKEOFF;
        }
        if (phase == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.SITTING_FLAMING) {
            return Phase.SITTING_FLAMING;
        }
        if (phase == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.SITTING_SCANNING) {
            return Phase.SITTING_SCANNING;
        }
        if (phase == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.SITTING_ATTACKING) {
            return Phase.SITTING_ATTACKING;
        }
        if (phase == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.CHARGING_PLAYER) {
            return Phase.CHARGING_PLAYER;
        }
        if (phase == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.DYING) {
            return Phase.DYING;
        }
        return Phase.HOVERING;
    }

    private static net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase<?> toVanilla(Phase phase) {
        return switch (phase) {
            case HOLDING_PATTERN -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN;
            case STRAFE_PLAYER -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.STRAFE_PLAYER;
            case LANDING_APPROACH -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.LANDING_APPROACH;
            case LANDING -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.LANDING;
            case TAKEOFF -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.TAKEOFF;
            case SITTING_FLAMING -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.SITTING_FLAMING;
            case SITTING_SCANNING -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.SITTING_SCANNING;
            case SITTING_ATTACKING -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.SITTING_ATTACKING;
            case CHARGING_PLAYER -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.CHARGING_PLAYER;
            case DYING -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.DYING;
            case HOVERING -> net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOVERING;
        };
    }
}
