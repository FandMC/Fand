package io.fand.api.entity;

import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla neutral mob anger state used by entities such as bees and wolves.
 */
public interface Angerable extends Mob {

    boolean angry();

    long angerEndTime();

    void setAngerEndTime(long gameTime);

    default long angerTicksRemaining(long currentGameTime) {
        return Math.max(0L, angerEndTime() - currentGameTime);
    }

    void startAngerTimer();

    Optional<UUID> angerTargetId();

    void setAngerTarget(@Nullable LivingEntity target);

    void clearAnger();
}
