package io.fand.api.item.component;

import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * Vanilla enchantment registry keys.
 */
public final class EnchantmentKeys {

    public static final Key PROTECTION = vanilla("protection");
    public static final Key FIRE_PROTECTION = vanilla("fire_protection");
    public static final Key FEATHER_FALLING = vanilla("feather_falling");
    public static final Key BLAST_PROTECTION = vanilla("blast_protection");
    public static final Key PROJECTILE_PROTECTION = vanilla("projectile_protection");
    public static final Key RESPIRATION = vanilla("respiration");
    public static final Key AQUA_AFFINITY = vanilla("aqua_affinity");
    public static final Key THORNS = vanilla("thorns");
    public static final Key DEPTH_STRIDER = vanilla("depth_strider");
    public static final Key FROST_WALKER = vanilla("frost_walker");
    public static final Key BINDING_CURSE = vanilla("binding_curse");
    public static final Key SOUL_SPEED = vanilla("soul_speed");
    public static final Key SWIFT_SNEAK = vanilla("swift_sneak");
    public static final Key SHARPNESS = vanilla("sharpness");
    public static final Key SMITE = vanilla("smite");
    public static final Key BANE_OF_ARTHROPODS = vanilla("bane_of_arthropods");
    public static final Key KNOCKBACK = vanilla("knockback");
    public static final Key FIRE_ASPECT = vanilla("fire_aspect");
    public static final Key LOOTING = vanilla("looting");
    public static final Key SWEEPING_EDGE = vanilla("sweeping_edge");
    public static final Key EFFICIENCY = vanilla("efficiency");
    public static final Key SILK_TOUCH = vanilla("silk_touch");
    public static final Key UNBREAKING = vanilla("unbreaking");
    public static final Key FORTUNE = vanilla("fortune");
    public static final Key POWER = vanilla("power");
    public static final Key PUNCH = vanilla("punch");
    public static final Key FLAME = vanilla("flame");
    public static final Key INFINITY = vanilla("infinity");
    public static final Key LUCK_OF_THE_SEA = vanilla("luck_of_the_sea");
    public static final Key LURE = vanilla("lure");
    public static final Key LOYALTY = vanilla("loyalty");
    public static final Key IMPALING = vanilla("impaling");
    public static final Key RIPTIDE = vanilla("riptide");
    public static final Key CHANNELING = vanilla("channeling");
    public static final Key MULTISHOT = vanilla("multishot");
    public static final Key QUICK_CHARGE = vanilla("quick_charge");
    public static final Key PIERCING = vanilla("piercing");
    public static final Key DENSITY = vanilla("density");
    public static final Key BREACH = vanilla("breach");
    public static final Key WIND_BURST = vanilla("wind_burst");
    public static final Key LUNGE = vanilla("lunge");
    public static final Key MENDING = vanilla("mending");
    public static final Key VANISHING_CURSE = vanilla("vanishing_curse");

    private static final Set<Key> ALL = Set.of(
            PROTECTION,
            FIRE_PROTECTION,
            FEATHER_FALLING,
            BLAST_PROTECTION,
            PROJECTILE_PROTECTION,
            RESPIRATION,
            AQUA_AFFINITY,
            THORNS,
            DEPTH_STRIDER,
            FROST_WALKER,
            BINDING_CURSE,
            SOUL_SPEED,
            SWIFT_SNEAK,
            SHARPNESS,
            SMITE,
            BANE_OF_ARTHROPODS,
            KNOCKBACK,
            FIRE_ASPECT,
            LOOTING,
            SWEEPING_EDGE,
            EFFICIENCY,
            SILK_TOUCH,
            UNBREAKING,
            FORTUNE,
            POWER,
            PUNCH,
            FLAME,
            INFINITY,
            LUCK_OF_THE_SEA,
            LURE,
            LOYALTY,
            IMPALING,
            RIPTIDE,
            CHANNELING,
            MULTISHOT,
            QUICK_CHARGE,
            PIERCING,
            DENSITY,
            BREACH,
            WIND_BURST,
            LUNGE,
            MENDING,
            VANISHING_CURSE);

    private EnchantmentKeys() {
    }

    public static Set<Key> all() {
        return ALL;
    }

    public static boolean isVanilla(Key key) {
        return ALL.contains(key);
    }

    private static Key vanilla(String id) {
        return Key.key("minecraft:" + id);
    }
}
