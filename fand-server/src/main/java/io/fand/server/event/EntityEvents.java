package io.fand.server.event;

import io.fand.api.event.entity.EntityDeathEvent;
import io.fand.server.hooks.FandHooks;
import java.util.Optional;
import net.minecraft.world.damagesource.DamageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntityEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEvents.class);

    private EntityEvents() {
    }

    public static void fireDeath(net.minecraft.world.entity.LivingEntity entity, DamageSource source) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityDeathEvent.class)) {
            return;
        }
        var fandEntity = FandHooks.wrapLivingEntity(entity);
        if (fandEntity == null) {
            return;
        }
        var directEntity = Optional.ofNullable(source.getDirectEntity())
                .filter(candidate -> candidate instanceof net.minecraft.world.entity.LivingEntity)
                .map(candidate -> FandHooks.wrapLivingEntity((net.minecraft.world.entity.LivingEntity) candidate));
        var attacker = Optional.ofNullable(source.getEntity())
                .filter(candidate -> candidate instanceof net.minecraft.world.entity.LivingEntity)
                .map(candidate -> FandHooks.wrapLivingEntity((net.minecraft.world.entity.LivingEntity) candidate));
        var cause = source.typeHolder().unwrapKey().map(key -> key.identifier().toString()).orElse("minecraft:generic");
        try {
            bus.fire(new EntityDeathEvent(fandEntity, cause, directEntity, attacker));
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityDeathEvent listener failed", failure);
        }
    }
}
