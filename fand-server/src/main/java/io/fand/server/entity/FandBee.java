package io.fand.server.entity;

import io.fand.api.entity.Bee;
import io.fand.api.entity.LivingEntity;
import io.fand.api.world.Location;
import io.fand.server.util.ReflectionFields;
import io.fand.server.world.WorldRegistry;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

public final class FandBee extends FandAnimal implements Bee {
    private static final Method SET_HAS_NECTAR = ReflectionFields.method(
            net.minecraft.world.entity.animal.bee.Bee.class,
            "setHasNectar",
            boolean.class);
    private static final Method SET_HAS_STUNG = ReflectionFields.method(
            net.minecraft.world.entity.animal.bee.Bee.class,
            "setHasStung",
            boolean.class);

    public FandBee(net.minecraft.world.entity.animal.bee.Bee handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.bee.Bee handle() {
        return (net.minecraft.world.entity.animal.bee.Bee) handle;
    }

    @Override
    public boolean angry() {
        return FandAngerable.angry(handle());
    }

    @Override
    public long angerEndTime() {
        return FandAngerable.angerEndTime(handle());
    }

    @Override
    public void setAngerEndTime(long gameTime) {
        runOnServerThread(() -> FandAngerable.setAngerEndTime(handle(), gameTime));
    }

    @Override
    public void startAngerTimer() {
        runOnServerThread(() -> FandAngerable.startAngerTimer(handle()));
    }

    @Override
    public Optional<UUID> angerTargetId() {
        return FandAngerable.angerTargetId(handle());
    }

    @Override
    public void setAngerTarget(@Nullable LivingEntity target) {
        runOnServerThread(() -> FandAngerable.setAngerTarget(handle(), target));
    }

    @Override
    public void clearAnger() {
        runOnServerThread(() -> FandAngerable.clearAnger(handle()));
    }

    @Override
    public boolean hasHive() {
        return handle().hasHive();
    }

    @Override
    public Optional<Location> hiveLocation() {
        return Optional.ofNullable(handle().getHivePos())
                .map(pos -> new Location(world(), pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F));
    }

    @Override
    public void setHiveLocation(@Nullable Location location) {
        runOnServerThread(() -> handle().setHivePos(location == null
                ? null
                : BlockPos.containing(location.x(), location.y(), location.z())));
    }

    @Override
    public boolean hasNectar() {
        return handle().hasNectar();
    }

    @Override
    public void setHasNectar(boolean hasNectar) {
        runOnServerThread(() -> ReflectionFields.invoke(SET_HAS_NECTAR, handle(), hasNectar));
    }

    @Override
    public boolean hasStung() {
        return handle().hasStung();
    }

    @Override
    public void setHasStung(boolean hasStung) {
        runOnServerThread(() -> ReflectionFields.invoke(SET_HAS_STUNG, handle(), hasStung));
    }

    @Override
    public void setStayOutOfHiveTicks(int ticks) {
        runOnServerThread(() -> handle().setStayOutOfHiveCountdown(Math.max(0, ticks)));
    }
}
