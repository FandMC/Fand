package io.fand.server.entity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fand.server.world.WorldRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.time.Duration;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Caches API wrappers around vanilla entities so listeners observe a stable
 * identity across consecutive event fires for the same entity.
 *
 * <p>Players are delegated to {@link PlayerRegistry}; non-player entries expire
 * after idle access because they do not have explicit attach/detach hooks here.
 */
public final class EntityRegistry {

    private static final int NETWORK_ID_WRAPPER_CACHE_MAX_SIZE = 8192;

    private final WorldRegistry worldRegistry;
    private final PlayerRegistry players;
    private final Int2ObjectOpenHashMap<FandEntity> wrappersByNetworkId = new Int2ObjectOpenHashMap<>();
    private final Cache<UUID, FandEntity> wrappers = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(8192)
            .build();

    public EntityRegistry(WorldRegistry worldRegistry, PlayerRegistry players) {
        this.worldRegistry = worldRegistry;
        this.players = players;
    }

    public io.fand.api.entity.Entity wrap(net.minecraft.world.entity.Entity handle) {
        if (handle instanceof ServerPlayer player) {
            var existing = players.findOrNull(player.getUUID());
            return existing != null ? existing : players.attach(player);
        }
        int networkId = handle.getId();
        var existingByNetworkId = wrappersByNetworkId.get(networkId);
        if (existingByNetworkId != null && existingByNetworkId.handle() == handle) {
            return existingByNetworkId;
        }
        var existing = wrappers.getIfPresent(handle.getUUID());
        if (existing != null && existing.handle() == handle) {
            rememberNetworkId(networkId, existing);
            return existing;
        }
        var fresh = wrapFresh(handle);
        wrappers.put(handle.getUUID(), fresh);
        rememberNetworkId(networkId, fresh);
        return fresh;
    }

    public io.fand.api.entity.LivingEntity wrap(LivingEntity handle) {
        var wrapped = wrap((net.minecraft.world.entity.Entity) handle);
        if (wrapped instanceof io.fand.api.entity.LivingEntity living) {
            return living;
        }
        throw new IllegalStateException("Living entity wrapped as non-living entity: " + handle);
    }

    private void rememberNetworkId(int networkId, FandEntity wrapper) {
        if (wrappersByNetworkId.size() >= NETWORK_ID_WRAPPER_CACHE_MAX_SIZE && !wrappersByNetworkId.containsKey(networkId)) {
            wrappersByNetworkId.clear();
        }
        wrappersByNetworkId.put(networkId, wrapper);
    }

    private FandEntity wrapFresh(net.minecraft.world.entity.Entity handle) {
        if (handle instanceof net.minecraft.world.entity.decoration.ArmorStand armorStand) {
            return new FandArmorStand(armorStand, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
            return new FandEnderDragon(dragon, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.AreaEffectCloud cloud) {
            return new FandAreaEffectCloud(cloud, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.item.PrimedTnt explosive) {
            return new FandExplosive(explosive, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.ExperienceOrb orb) {
            return new FandExperienceOrb(orb, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.Display.BlockDisplay display) {
            return new FandBlockDisplay(display, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.Display.ItemDisplay display) {
            return new FandItemDisplay(display, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.Display.TextDisplay display) {
            return new FandTextDisplay(display, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.Display display) {
            return new FandDisplay(display, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.decoration.BlockAttachedEntity hanging) {
            return new FandHanging(hanging, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.vehicle.minecart.AbstractMinecart minecart) {
            return new FandMinecart(minecart, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.vehicle.VehicleEntity vehicle) {
            return new FandVehicle(vehicle, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.item.ItemEntity item) {
            return new FandItemEntity(item, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            return new FandProjectile(projectile, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.TamableAnimal tameable) {
            return new FandTameable(tameable, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.npc.villager.Villager villager) {
            return new FandVillager(villager, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse) {
            return new FandHorse(horse, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.animal.Animal animal) {
            return new FandAnimal(animal, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.AgeableMob ageable) {
            return new FandAgeable(ageable, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.monster.EnderMan enderman) {
            return new FandEnderman(enderman, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.monster.Creeper creeper) {
            return new FandCreeper(creeper, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.Mob mob) {
            return new FandMob(mob, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.LivingEntity living) {
            return new FandLivingEntity(living, worldRegistry);
        }
        return new FandEntity(handle, worldRegistry);
    }
}
