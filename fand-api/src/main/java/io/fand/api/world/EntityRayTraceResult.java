package io.fand.api.world;

import io.fand.api.entity.Entity;
import java.util.Objects;

/**
 * Result of a world entity ray trace.
 */
public record EntityRayTraceResult(Entity entity, Location hitLocation, double distance) {

    public EntityRayTraceResult {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(hitLocation, "hitLocation");
        if (!Double.isFinite(distance) || distance < 0.0) {
            throw new IllegalArgumentException("distance must be finite and >= 0");
        }
    }
}
