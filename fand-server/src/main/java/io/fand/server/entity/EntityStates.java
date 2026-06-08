package io.fand.server.entity;

import io.fand.server.command.AdventureBridge;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

final class EntityStates {

    private EntityStates() {
    }

    static Optional<Component> customName(net.minecraft.world.entity.Entity entity) {
        return Optional.ofNullable(entity.getCustomName())
                .map(name -> AdventureBridge.fromVanilla(name, entity.registryAccess()));
    }

    static void setCustomName(net.minecraft.world.entity.Entity entity, @Nullable Component name) {
        entity.setCustomName(name == null ? null : AdventureBridge.toVanilla(name, entity.registryAccess()));
    }

    static boolean customNameVisible(net.minecraft.world.entity.Entity entity) {
        return entity.isCustomNameVisible();
    }

    static void setCustomNameVisible(net.minecraft.world.entity.Entity entity, boolean visible) {
        entity.setCustomNameVisible(visible);
    }

    static boolean glowing(net.minecraft.world.entity.Entity entity) {
        return entity.isCurrentlyGlowing();
    }

    static void setGlowing(net.minecraft.world.entity.Entity entity, boolean glowing) {
        entity.setGlowingTag(glowing);
    }

    static boolean silent(net.minecraft.world.entity.Entity entity) {
        return entity.isSilent();
    }

    static void setSilent(net.minecraft.world.entity.Entity entity, boolean silent) {
        entity.setSilent(silent);
    }

    static boolean gravity(net.minecraft.world.entity.Entity entity) {
        return !entity.isNoGravity();
    }

    static void setGravity(net.minecraft.world.entity.Entity entity, boolean gravity) {
        entity.setNoGravity(!gravity);
    }

    static boolean invulnerable(net.minecraft.world.entity.Entity entity) {
        return entity.isInvulnerable();
    }

    static void setInvulnerable(net.minecraft.world.entity.Entity entity, boolean invulnerable) {
        entity.setInvulnerable(invulnerable);
    }

    static Set<String> scoreboardTags(net.minecraft.world.entity.Entity entity) {
        return Set.copyOf(entity.entityTags());
    }

    static void addScoreboardTag(net.minecraft.world.entity.Entity entity, String tag) {
        entity.addTag(tag);
    }

    static void removeScoreboardTag(net.minecraft.world.entity.Entity entity, String tag) {
        entity.removeTag(tag);
    }

    static double width(net.minecraft.world.entity.Entity entity) {
        return entity.getBbWidth();
    }

    static double height(net.minecraft.world.entity.Entity entity) {
        return entity.getBbHeight();
    }
}
