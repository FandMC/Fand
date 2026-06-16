package io.fand.api.event.entity;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Typed vanilla damage cause identifier.
 */
public record DamageCause(Key key) {

    public static final DamageCause GENERIC = minecraft("generic");
    public static final DamageCause PLAYER_ATTACK = minecraft("player_attack");
    public static final DamageCause MOB_ATTACK = minecraft("mob_attack");
    public static final DamageCause MOB_ATTACK_NO_AGGRO = minecraft("mob_attack_no_aggro");
    public static final DamageCause PROJECTILE = minecraft("arrow");
    public static final DamageCause ARROW = minecraft("arrow");
    public static final DamageCause TRIDENT = minecraft("trident");
    public static final DamageCause MOB_PROJECTILE = minecraft("mob_projectile");
    public static final DamageCause FIREBALL = minecraft("fireball");
    public static final DamageCause UNATTRIBUTED_FIREBALL = minecraft("unattributed_fireball");
    public static final DamageCause WITHER_SKULL = minecraft("wither_skull");
    public static final DamageCause THROWN = minecraft("thrown");
    public static final DamageCause FALL = minecraft("fall");
    public static final DamageCause ENDER_PEARL = minecraft("ender_pearl");
    public static final DamageCause FLY_INTO_WALL = minecraft("fly_into_wall");
    public static final DamageCause STALAGMITE = minecraft("stalagmite");
    public static final DamageCause FIRE = minecraft("in_fire");
    public static final DamageCause IN_FIRE = minecraft("in_fire");
    public static final DamageCause CAMPFIRE = minecraft("campfire");
    public static final DamageCause LIGHTNING_BOLT = minecraft("lightning_bolt");
    public static final DamageCause ON_FIRE = minecraft("on_fire");
    public static final DamageCause HOT_FLOOR = minecraft("hot_floor");
    public static final DamageCause SULFUR_CUBE_HOT = minecraft("sulfur_cube_hot");
    public static final DamageCause LAVA = minecraft("lava");
    public static final DamageCause IN_WALL = minecraft("in_wall");
    public static final DamageCause CRAMMING = minecraft("cramming");
    public static final DamageCause DROWN = minecraft("drown");
    public static final DamageCause STARVE = minecraft("starve");
    public static final DamageCause CACTUS = minecraft("cactus");
    public static final DamageCause DRY_OUT = minecraft("dry_out");
    public static final DamageCause SWEET_BERRY_BUSH = minecraft("sweet_berry_bush");
    public static final DamageCause EXPLOSION = minecraft("explosion");
    public static final DamageCause PLAYER_EXPLOSION = minecraft("player_explosion");
    public static final DamageCause MAGIC = minecraft("magic");
    public static final DamageCause WITHER = minecraft("wither");
    public static final DamageCause DRAGON_BREATH = minecraft("dragon_breath");
    public static final DamageCause INDIRECT_MAGIC = minecraft("indirect_magic");
    public static final DamageCause THORNS = minecraft("thorns");
    public static final DamageCause FREEZE = minecraft("freeze");
    public static final DamageCause OUT_OF_WORLD = minecraft("out_of_world");
    public static final DamageCause FALLING_BLOCK = minecraft("falling_block");
    public static final DamageCause FALLING_ANVIL = minecraft("falling_anvil");
    public static final DamageCause FALLING_STALACTITE = minecraft("falling_stalactite");
    public static final DamageCause STING = minecraft("sting");
    public static final DamageCause SPEAR = minecraft("spear");
    public static final DamageCause SPIT = minecraft("spit");
    public static final DamageCause WIND_CHARGE = minecraft("wind_charge");
    public static final DamageCause FIREWORKS = minecraft("fireworks");
    public static final DamageCause SONIC_BOOM = minecraft("sonic_boom");
    public static final DamageCause BAD_RESPAWN_POINT = minecraft("bad_respawn_point");
    public static final DamageCause OUTSIDE_BORDER = minecraft("outside_border");
    public static final DamageCause GENERIC_KILL = minecraft("generic_kill");
    public static final DamageCause MACE_SMASH = minecraft("mace_smash");

    public DamageCause {
        Objects.requireNonNull(key, "key");
    }

    public static DamageCause of(Key key) {
        return new DamageCause(key);
    }

    public static DamageCause of(String key) {
        return of(Key.key(key));
    }

    public static DamageCause minecraft(String value) {
        return of(Key.key(Key.MINECRAFT_NAMESPACE, value));
    }

    public String asString() {
        return key.asString();
    }
}
