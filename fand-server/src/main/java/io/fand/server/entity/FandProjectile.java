package io.fand.server.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Projectile;
import io.fand.api.world.Vector3;
import io.fand.server.world.WorldRegistry;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class FandProjectile extends FandEntity implements Projectile {

    public FandProjectile(net.minecraft.world.entity.projectile.Projectile handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.projectile.Projectile handle() {
        return (net.minecraft.world.entity.projectile.Projectile) handle;
    }

    @Override
    public Optional<? extends Entity> shooter() {
        return Optional.ofNullable(handle().getOwner()).map(worldRegistry.entityRegistry()::wrap);
    }

    @Override
    public void setShooter(@Nullable Entity shooter) {
        runOnServerThread(() -> handle().setOwner(shooter == null ? null : EntityHandles.unwrap(shooter)));
    }

    @Override
    public void shoot(Vector3 direction, double power, double uncertainty) {
        java.util.Objects.requireNonNull(direction, "direction");
        runOnServerThread(() -> handle().shoot(
                direction.x(),
                direction.y(),
                direction.z(),
                (float) power,
                (float) uncertainty));
    }
}
