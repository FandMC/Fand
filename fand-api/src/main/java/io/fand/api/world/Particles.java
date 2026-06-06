package io.fand.api.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;

/**
 * Registry of common vanilla particle types.
 *
 * <p>Particle instances are lazily constructed and cached. Custom particles can
 * be obtained via {@link #particle(Key)}.
 */
public final class Particles {

    private static final Map<String, Particle> CACHE = new ConcurrentHashMap<>();

    private Particles() {}

    public static Particle particle(Key key) {
        return particle(key.asString());
    }

    /**
     * Creates a particle from a vanilla particle argument string. The string may
     * be a simple key such as {@code minecraft:flame}, or a fully parameterized
     * vanilla particle such as {@code minecraft:dust{color:16711680,scale:1.0}}.
     */
    public static Particle particle(String argument) {
        return CACHE.computeIfAbsent(argument, ParticleImpl::of);
    }

    public static Particle dust(int rgb, float scale) {
        return particle("minecraft:dust{color:" + rgb + ",scale:" + scale + "}");
    }

    public static Particle dust(float red, float green, float blue, float scale) {
        return dust(rgb(red, green, blue), scale);
    }

    public static Particle dustTransition(int fromRgb, int toRgb, float scale) {
        return particle("minecraft:dust_color_transition{from_color:" + fromRgb
                + ",to_color:" + toRgb + ",scale:" + scale + "}");
    }

    public static Particle block(String blockState) {
        return particle("minecraft:block{block_state:\"" + escape(blockState) + "\"}");
    }

    public static Particle fallingDust(String blockState) {
        return particle("minecraft:falling_dust{block_state:\"" + escape(blockState) + "\"}");
    }

    public static Particle item(String itemKey) {
        return particle("minecraft:item{item:{id:\"" + escape(itemKey) + "\"}}");
    }

    public static Particle shriek(int delay) {
        return particle("minecraft:shriek{delay:" + delay + "}");
    }

    public static Particle sculkCharge(float roll) {
        return particle("minecraft:sculk_charge{roll:" + roll + "}");
    }

    public static Particle FLAME = particle(Key.key("minecraft:flame"));
    public static Particle SMOKE = particle(Key.key("minecraft:smoke"));
    public static Particle HEART = particle(Key.key("minecraft:heart"));
    public static Particle EXPLOSION = particle(Key.key("minecraft:explosion"));
    public static Particle CRIT = particle(Key.key("minecraft:crit"));
    public static Particle ENCHANTED_HIT = particle(Key.key("minecraft:enchanted_hit"));
    public static Particle PORTAL = particle(Key.key("minecraft:portal"));
    public static Particle CLOUD = particle(Key.key("minecraft:cloud"));
    public static Particle DUST = particle(Key.key("minecraft:dust"));
    public static Particle DRIPPING_WATER = particle(Key.key("minecraft:dripping_water"));
    public static Particle DRIPPING_LAVA = particle(Key.key("minecraft:dripping_lava"));
    public static Particle NOTE = particle(Key.key("minecraft:note"));
    public static Particle HAPPY_VILLAGER = particle(Key.key("minecraft:happy_villager"));
    public static Particle ANGRY_VILLAGER = particle(Key.key("minecraft:angry_villager"));
    public static Particle TOTEM_OF_UNDYING = particle(Key.key("minecraft:totem_of_undying"));
    public static Particle END_ROD = particle(Key.key("minecraft:end_rod"));
    public static Particle DRAGON_BREATH = particle(Key.key("minecraft:dragon_breath"));
    public static Particle DAMAGE_INDICATOR = particle(Key.key("minecraft:damage_indicator"));
    public static Particle FIREWORK = particle(Key.key("minecraft:firework"));
    public static Particle GLOW = particle(Key.key("minecraft:glow"));
    public static Particle SOUL = particle(Key.key("minecraft:soul"));
    public static Particle SOUL_FIRE_FLAME = particle(Key.key("minecraft:soul_fire_flame"));
    public static Particle CHERRY_LEAVES = particle(Key.key("minecraft:cherry_leaves"));
    public static Particle TRIAL_SPAWNER_DETECTION = particle(Key.key("minecraft:trial_spawner_detection"));

    private static int rgb(float red, float green, float blue) {
        int r = Math.round(clamp01(red) * 255.0F);
        int g = Math.round(clamp01(green) * 255.0F);
        int b = Math.round(clamp01(blue) * 255.0F);
        return (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record ParticleImpl(Key key, String argument) implements Particle {
        static Particle of(String argument) {
            int dataStart = argument.indexOf('{');
            String keyText = dataStart < 0 ? argument : argument.substring(0, dataStart);
            return new ParticleImpl(Key.key(keyText), argument);
        }
    }
}
