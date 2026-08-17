package io.fand.api.block;

import io.fand.api.entity.CopperGolem;
import java.util.Optional;

/** Copper Golem Statue pose and awakening controls. */
public interface CopperGolemStatueBlockEntity extends BlockEntity {
    enum Pose { STANDING, SITTING, RUNNING, STAR }
    Pose pose();
    void setPose(Pose pose);
    Optional<? extends CopperGolem> awaken();
}
