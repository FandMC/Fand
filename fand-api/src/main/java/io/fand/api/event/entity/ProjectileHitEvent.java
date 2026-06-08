package io.fand.api.event.entity;

import io.fand.api.block.Block;
import io.fand.api.entity.Entity;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread when a projectile collides with a block or entity.
 */
public final class ProjectileHitEvent implements Event {

    private final Entity projectile;
    private final Optional<Entity> hitEntity;
    private final Optional<Block> hitBlock;
    private final Location hitLocation;
    private final HitType hitType;

    public ProjectileHitEvent(
            Entity projectile,
            Optional<Entity> hitEntity,
            Optional<Block> hitBlock,
            Location hitLocation,
            HitType hitType
    ) {
        this.projectile = Objects.requireNonNull(projectile, "projectile");
        this.hitEntity = Objects.requireNonNull(hitEntity, "hitEntity");
        this.hitBlock = Objects.requireNonNull(hitBlock, "hitBlock");
        this.hitLocation = Objects.requireNonNull(hitLocation, "hitLocation");
        this.hitType = Objects.requireNonNull(hitType, "hitType");
    }

    public Entity projectile() {
        return projectile;
    }

    public Optional<Entity> hitEntity() {
        return hitEntity;
    }

    public Optional<Block> hitBlock() {
        return hitBlock;
    }

    public Location hitLocation() {
        return hitLocation;
    }

    public HitType hitType() {
        return hitType;
    }

    public enum HitType {
        BLOCK,
        ENTITY,
        MISS
    }
}
