package io.fand.api.world;

import io.fand.api.block.BlockType;
import io.fand.api.item.ItemType;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;

/**
 * Registry of common vanilla particle types.
 *
 * <p>Particle instances are lazily constructed and cached. Custom simple
 * particles can be obtained via {@link #particle(Key)}. Fully parameterized
 * vanilla particle argument strings are available through {@link #raw(String)}
 * as an explicit escape hatch.
 */
public final class Particles {

    private static final Map<String, Particle> CACHE = new ConcurrentHashMap<>();

    private Particles() {}

    public static Particle particle(Key key) {
        Objects.requireNonNull(key, "key");
        return CACHE.computeIfAbsent(key.asString(), ParticleImpl::of);
    }

    /**
     * Creates a particle from a vanilla particle argument string. This is an
     * explicit escape hatch for data-bearing particles such as
     * {@code minecraft:dust{color:16711680,scale:1.0}}.
     */
    public static Particle raw(String argument) {
        return CACHE.computeIfAbsent(argument, ParticleImpl::of);
    }

    /**
     * @deprecated Use {@link #particle(Key)} for simple keys or {@link #raw(String)}
     * for full vanilla particle argument strings.
     */
    @Deprecated(forRemoval = false)
    public static Particle particle(String argument) {
        return raw(argument);
    }

    public static Particle dust(int rgb, float scale) {
        requireRgb(rgb, "rgb");
        requirePositiveFinite(scale, "scale");
        return raw("minecraft:dust{color:" + rgb + ",scale:" + scale + "}");
    }

    public static Particle dust(float red, float green, float blue, float scale) {
        return dust(rgb(red, green, blue), scale);
    }

    public static Particle dustTransition(int fromRgb, int toRgb, float scale) {
        requireRgb(fromRgb, "fromRgb");
        requireRgb(toRgb, "toRgb");
        requirePositiveFinite(scale, "scale");
        return raw("minecraft:dust_color_transition{from_color:" + fromRgb
                + ",to_color:" + toRgb + ",scale:" + scale + "}");
    }

    public static Particle block(String blockState) {
        return raw("minecraft:block{block_state:\"" + escape(blockState) + "\"}");
    }

    public static Particle block(BlockType blockType) {
        Objects.requireNonNull(blockType, "blockType");
        return block(blockType.key().asString());
    }

    public static Particle fallingDust(String blockState) {
        return raw("minecraft:falling_dust{block_state:\"" + escape(blockState) + "\"}");
    }

    public static Particle fallingDust(BlockType blockType) {
        Objects.requireNonNull(blockType, "blockType");
        return fallingDust(blockType.key().asString());
    }

    public static Particle item(String itemKey) {
        return raw("minecraft:item{item:{id:\"" + escape(itemKey) + "\"}}");
    }

    public static Particle item(ItemType itemType) {
        Objects.requireNonNull(itemType, "itemType");
        return item(itemType.key().asString());
    }

    public static Particle shriek(int delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("delay must be >= 0, got " + delay);
        }
        return raw("minecraft:shriek{delay:" + delay + "}");
    }

    public static Particle sculkCharge(float roll) {
        requireFinite(roll, "roll");
        return raw("minecraft:sculk_charge{roll:" + roll + "}");
    }

    public static final Particle FLAME = particle(Key.key("minecraft:flame"));
    public static final Particle SMOKE = particle(Key.key("minecraft:smoke"));
    public static final Particle HEART = particle(Key.key("minecraft:heart"));
    public static final Particle EXPLOSION = particle(Key.key("minecraft:explosion"));
    public static final Particle CRIT = particle(Key.key("minecraft:crit"));
    public static final Particle ENCHANTED_HIT = particle(Key.key("minecraft:enchanted_hit"));
    public static final Particle PORTAL = particle(Key.key("minecraft:portal"));
    public static final Particle CLOUD = particle(Key.key("minecraft:cloud"));
    public static final Particle DUST = particle(Key.key("minecraft:dust"));
    public static final Particle DRIPPING_WATER = particle(Key.key("minecraft:dripping_water"));
    public static final Particle DRIPPING_LAVA = particle(Key.key("minecraft:dripping_lava"));
    public static final Particle NOTE = particle(Key.key("minecraft:note"));
    public static final Particle HAPPY_VILLAGER = particle(Key.key("minecraft:happy_villager"));
    public static final Particle ANGRY_VILLAGER = particle(Key.key("minecraft:angry_villager"));
    public static final Particle TOTEM_OF_UNDYING = particle(Key.key("minecraft:totem_of_undying"));
    public static final Particle END_ROD = particle(Key.key("minecraft:end_rod"));
    public static final Particle DRAGON_BREATH = particle(Key.key("minecraft:dragon_breath"));
    public static final Particle DAMAGE_INDICATOR = particle(Key.key("minecraft:damage_indicator"));
    public static final Particle FIREWORK = particle(Key.key("minecraft:firework"));
    public static final Particle GLOW = particle(Key.key("minecraft:glow"));
    public static final Particle SOUL = particle(Key.key("minecraft:soul"));
    public static final Particle SOUL_FIRE_FLAME = particle(Key.key("minecraft:soul_fire_flame"));
    public static final Particle CHERRY_LEAVES = particle(Key.key("minecraft:cherry_leaves"));
    public static final Particle TRIAL_SPAWNER_DETECTION = particle(Key.key("minecraft:trial_spawner_detection"));

    private static int rgb(float red, float green, float blue) {
        requireUnitFinite(red, "red");
        requireUnitFinite(green, "green");
        requireUnitFinite(blue, "blue");
        int r = Math.round(red * 255.0F);
        int g = Math.round(green * 255.0F);
        int b = Math.round(blue * 255.0F);
        return (r << 16) | (g << 8) | b;
    }

    private static void requireRgb(int value, String name) {
        if (value < 0x000000 || value > 0xFFFFFF) {
            throw new IllegalArgumentException(name + " must be in [0x000000, 0xFFFFFF], got " + value);
        }
    }

    private static void requireUnitFinite(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F || value > 1.0F) {
            throw new IllegalArgumentException(name + " must be in [0, 1], got " + value);
        }
    }

    private static void requirePositiveFinite(float value, String name) {
        requireFinite(value, name);
        if (value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be > 0, got " + value);
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite, got " + value);
        }
    }

    private static String escape(String value) {
        Objects.requireNonNull(value, "value");
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record ParticleImpl(Key key, String argument) implements Particle {
        static Particle of(String argument) {
            Objects.requireNonNull(argument, "argument");
            int dataStart = argument.indexOf('{');
            String keyText = dataStart < 0 ? argument : argument.substring(0, dataStart);
            return new ParticleImpl(Key.key(keyText), argument);
        }
    }
}
