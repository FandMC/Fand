package io.fand.api.entity;

import io.fand.api.world.Vector3;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Entity with projectile ownership and launch controls.
 */
public interface Projectile extends Entity {

    Optional<? extends Entity> shooter();

    void setShooter(@Nullable Entity shooter);

    void shoot(Vector3 direction, double power, double uncertainty);
}
