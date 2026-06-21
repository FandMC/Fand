package io.fand.api.entity;

import java.util.Optional;

public interface Warden extends Mob {

    enum AngerLevel {
        CALM,
        AGITATED,
        ANGRY
    }

    AngerLevel angerLevel();

    int anger();

    Optional<? extends LivingEntity> activeAngerTarget();

    void increaseAnger(@org.jspecify.annotations.Nullable Entity entity, int amount, boolean playSound);

    void clearAnger(Entity entity);
}
