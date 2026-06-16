package io.fand.server.entity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fand.server.world.WorldRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
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
 *
 * <p>Not thread-safe: the network-id map is only touched from the server
 * thread (all wrap calls originate from patched main-thread event sites).
 */
public final class EntityRegistry {

    private static final int NETWORK_ID_WRAPPER_CACHE_MAX_SIZE = 8192;

    private final WorldRegistry worldRegistry;
    private final PlayerRegistry players;
    // Linked map maintains access order manually via getAndMoveToLast, so a
    // full cache evicts its least-recently-used entry instead of clearing
    // wholesale (which caused hit-rate collapse on entity-heavy servers).
    private final Int2ObjectLinkedOpenHashMap<FandEntity> wrappersByNetworkId = new Int2ObjectLinkedOpenHashMap<>();
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
        var existingByNetworkId = wrappersByNetworkId.getAndMoveToLast(networkId);
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
        wrappersByNetworkId.putAndMoveToLast(networkId, wrapper);
        if (wrappersByNetworkId.size() > NETWORK_ID_WRAPPER_CACHE_MAX_SIZE) {
            wrappersByNetworkId.removeFirst();
        }
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
        if (handle instanceof net.minecraft.world.entity.item.FallingBlockEntity fallingBlock) {
            return new FandFallingBlock(fallingBlock, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.Interaction interaction) {
            return new FandInteraction(interaction, worldRegistry);
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
        if (handle instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat boat) {
            return new FandBoat(boat, worldRegistry);
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
        if (handle instanceof net.minecraft.world.entity.animal.feline.Cat cat) {
            return new FandCat(cat, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf) {
            return new FandWolf(wolf, worldRegistry);
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
        if (handle instanceof net.minecraft.world.entity.animal.axolotl.Axolotl axolotl) {
            return new FandAxolotl(axolotl, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.animal.bee.Bee bee) {
            return new FandBee(bee, worldRegistry);
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
        if (handle instanceof net.minecraft.world.entity.monster.warden.Warden warden) {
            return new FandWarden(warden, worldRegistry);
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
