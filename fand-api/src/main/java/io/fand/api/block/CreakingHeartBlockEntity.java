package io.fand.api.block;

import io.fand.api.entity.LivingEntity;
import java.util.Optional;

/** Creaking Heart protector and comparator state. */
public interface CreakingHeartBlockEntity extends BlockEntity {
    Optional<? extends LivingEntity> protector();
    void removeProtector();
    int analogOutputSignal();
}
