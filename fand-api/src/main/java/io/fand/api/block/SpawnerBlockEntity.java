package io.fand.api.block;

import io.fand.api.entity.EntityType;
import java.util.Optional;

/**
 * Vanilla mob spawner block entity.
 */
public interface SpawnerBlockEntity extends BlockEntity {

    Optional<EntityType> spawnedType();

    boolean setSpawnedType(EntityType type);

    int delay();

    void setDelay(int ticks);

    int minDelay();

    void setMinDelay(int ticks);

    int maxDelay();

    void setMaxDelay(int ticks);

    default void setDelayRange(int minTicks, int maxTicks) {
        setMinDelay(minTicks);
        setMaxDelay(maxTicks);
    }

    int spawnCount();

    void setSpawnCount(int count);

    int maxNearbyEntities();

    void setMaxNearbyEntities(int count);

    int requiredPlayerRange();

    void setRequiredPlayerRange(int blocks);

    int spawnRange();

    void setSpawnRange(int blocks);
}
