package io.fand.datagenerator;

import java.util.List;

record DamageEventSpec(
        String className,
        String parentClass,
        String causeKey,
        String description,
        Payload payload,
        boolean acceptsCause,
        boolean routeExact
) {

    enum Payload {
        GENERIC,
        ENTITY,
        PLAYER,
        BLOCK
    }

    static List<DamageEventSpec> all() {
        return List.of(
                group("EntityProjectileDamageEvent", "EntityDamageEvent", "projectile damage"),
                exact("EntityArrowDamageEvent", "EntityProjectileDamageEvent", "minecraft:arrow", "arrow damage"),
                exact("EntityTridentDamageEvent", "EntityProjectileDamageEvent", "minecraft:trident", "trident damage"),
                exact("EntityMobProjectileDamageEvent", "EntityProjectileDamageEvent", "minecraft:mob_projectile", "generic mob projectile damage"),
                exact("EntitySpitDamageEvent", "EntityProjectileDamageEvent", "minecraft:spit", "llama spit damage"),
                exact("EntityFireballDamageEvent", "EntityProjectileDamageEvent", "minecraft:fireball", "attributed fireball damage"),
                exact("EntityUnattributedFireballDamageEvent", "EntityProjectileDamageEvent", "minecraft:unattributed_fireball", "unattributed fireball damage"),
                exact("EntityWitherSkullDamageEvent", "EntityProjectileDamageEvent", "minecraft:wither_skull", "wither skull damage"),
                exact("EntityThrownDamageEvent", "EntityProjectileDamageEvent", "minecraft:thrown", "thrown-projectile damage"),
                exact("EntityFireworksDamageEvent", "EntityProjectileDamageEvent", "minecraft:fireworks", "firework rocket damage"),
                exact("EntityWindChargeDamageEvent", "EntityProjectileDamageEvent", "minecraft:wind_charge", "wind charge damage"),

                group("EntityExplosionDamageEvent", "EntityDamageEvent", "explosion damage"),
                exact("EntityBadRespawnPointDamageEvent", "EntityExplosionDamageEvent", "minecraft:bad_respawn_point", "bad respawn point explosion damage"),

                group("EntityFallDamageEvent", "EntityDamageEvent", "fall-style damage"),
                exact("EntityEnderPearlDamageEvent", "EntityFallDamageEvent", "minecraft:ender_pearl", "ender pearl damage"),
                exact("EntityFlyIntoWallDamageEvent", "EntityFallDamageEvent", "minecraft:fly_into_wall", "fly-into-wall damage"),
                exact("EntityStalagmiteDamageEvent", "EntityFallDamageEvent", "minecraft:stalagmite", "stalagmite damage"),

                group("EntityFireDamageEvent", "EntityDamageEvent", "fire-style damage"),
                exact("EntityCampfireDamageEvent", "EntityFireDamageEvent", "minecraft:campfire", "campfire damage"),
                exact("EntityHotFloorDamageEvent", "EntityFireDamageEvent", "minecraft:hot_floor", "hot-floor damage"),
                exact("EntitySulfurCubeHotDamageEvent", "EntityFireDamageEvent", "minecraft:sulfur_cube_hot", "sulfur cube hot-floor damage"),
                exact("EntityLightningDamageEvent", "EntityFireDamageEvent", "minecraft:lightning_bolt", "lightning damage"),

                group("EntityMagicDamageEvent", "EntityDamageEvent", "magic-style damage"),
                exact("EntityWitherDamageEvent", "EntityMagicDamageEvent", "minecraft:wither", "wither effect damage"),
                exact("EntityDragonBreathDamageEvent", "EntityMagicDamageEvent", "minecraft:dragon_breath", "dragon breath damage"),

                entityGroup("EntityMobAttackDamageEvent", "EntityDamageByEntityEvent", "non-player living entity melee attack damage"),
                entityExact("EntityStingDamageEvent", "EntityMobAttackDamageEvent", "minecraft:sting", "sting damage"),
                entityExact("EntityMaceSmashDamageEvent", "EntityDamageByEntityEvent", "minecraft:mace_smash", "mace smash damage"),
                playerExact("EntityPlayerAttackDamageEvent", "EntityDamageByEntityEvent", "minecraft:player_attack", "player melee attack damage"),

                blockExact("EntityCactusDamageEvent", "EntityDamageByBlockEvent", "minecraft:cactus", "cactus damage"),
                exact("EntityLavaDamageEvent", "EntityDamageEvent", "minecraft:lava", "lava damage"),
                exact("EntityDrownDamageEvent", "EntityDamageEvent", "minecraft:drown", "drowning damage"),
                exact("EntityStarveDamageEvent", "EntityDamageEvent", "minecraft:starve", "starvation damage"),
                exact("EntityFreezeDamageEvent", "EntityDamageEvent", "minecraft:freeze", "freezing damage"),
                exact("EntityThornsDamageEvent", "EntityDamageEvent", "minecraft:thorns", "thorns damage"),
                exact("EntityOutOfWorldDamageEvent", "EntityDamageEvent", "minecraft:out_of_world", "out-of-world damage"),
                exact("EntitySonicBoomDamageEvent", "EntityDamageEvent", "minecraft:sonic_boom", "sonic boom damage"),
                exact("EntityOutsideBorderDamageEvent", "EntityDamageEvent", "minecraft:outside_border", "outside-border damage"),
                exact("EntityCrammingDamageEvent", "EntityDamageEvent", "minecraft:cramming", "entity cramming damage"),
                exact("EntityInWallDamageEvent", "EntityDamageEvent", "minecraft:in_wall", "in-wall suffocation damage"),
                exact("EntityDryOutDamageEvent", "EntityDamageEvent", "minecraft:dry_out", "dry-out damage"),
                exact("EntitySweetBerryBushDamageEvent", "EntityDamageEvent", "minecraft:sweet_berry_bush", "sweet berry bush damage"),
                exact("EntityFallingBlockDamageEvent", "EntityDamageEvent", "minecraft:falling_block", "falling block damage"),
                exact("EntityFallingAnvilDamageEvent", "EntityDamageEvent", "minecraft:falling_anvil", "falling anvil damage"),
                exact("EntityFallingStalactiteDamageEvent", "EntityDamageEvent", "minecraft:falling_stalactite", "falling stalactite damage"));
    }

    private static DamageEventSpec group(String className, String parentClass, String description) {
        return new DamageEventSpec(className, parentClass, null, description, Payload.GENERIC, true, false);
    }

    private static DamageEventSpec entityGroup(String className, String parentClass, String description) {
        return new DamageEventSpec(className, parentClass, null, description, Payload.ENTITY, true, false);
    }

    private static DamageEventSpec exact(String className, String parentClass, String causeKey, String description) {
        return new DamageEventSpec(className, parentClass, causeKey, description, Payload.GENERIC, false, true);
    }

    private static DamageEventSpec entityExact(String className, String parentClass, String causeKey, String description) {
        return new DamageEventSpec(className, parentClass, causeKey, description, Payload.ENTITY, false, true);
    }

    private static DamageEventSpec playerExact(String className, String parentClass, String causeKey, String description) {
        return new DamageEventSpec(className, parentClass, causeKey, description, Payload.PLAYER, false, true);
    }

    private static DamageEventSpec blockExact(String className, String parentClass, String causeKey, String description) {
        return new DamageEventSpec(className, parentClass, causeKey, description, Payload.BLOCK, false, true);
    }
}
