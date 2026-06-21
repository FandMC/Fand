package io.fand.server.entity;

import io.fand.api.entity.LivingEntity;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.NeutralMob;
import org.jspecify.annotations.Nullable;

final class FandAngerable {

    private FandAngerable() {
    }

    static boolean angry(NeutralMob handle) {
        return handle.isAngry();
    }

    static long angerEndTime(NeutralMob handle) {
        return handle.getPersistentAngerEndTime();
    }

    static void setAngerEndTime(NeutralMob handle, long gameTime) {
        handle.setPersistentAngerEndTime(gameTime);
    }

    static void startAngerTimer(NeutralMob handle) {
        handle.startPersistentAngerTimer();
    }

    static Optional<UUID> angerTargetId(NeutralMob handle) {
        return Optional.ofNullable(handle.getPersistentAngerTarget()).map(EntityReference::getUUID);
    }

    static void setAngerTarget(NeutralMob handle, @Nullable LivingEntity target) {
        var vanillaTarget = target == null ? null : (net.minecraft.world.entity.LivingEntity) EntityHandles.unwrap(target);
        handle.setPersistentAngerTarget(EntityReference.of(vanillaTarget));
        handle.setTarget(vanillaTarget);
    }

    static void clearAnger(NeutralMob handle) {
        handle.stopBeingAngry();
    }
}
