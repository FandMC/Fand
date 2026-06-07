package io.fand.api.world.particle;

import io.fand.api.block.BlockType;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Location;
import net.kyori.adventure.key.Key;

/** Factory methods for particle effects. */
public final class Particles {

    private Particles() {
    }

    public static SimpleParticleEffect simple(Key type) {
        return new SimpleParticleEffect(type);
    }

    public static SimpleParticleEffect simple(ParticleKey type) {
        return simple(type.key());
    }

    public static SimpleParticleEffect simple(String type) {
        return simple(Key.key(type));
    }

    public static BlockParticleEffect block(BlockType block) {
        return new BlockParticleEffect(Key.key("minecraft:block"), block);
    }

    public static BlockParticleEffect blockMarker(BlockType block) {
        return new BlockParticleEffect(Key.key("minecraft:block_marker"), block);
    }

    public static BlockParticleEffect fallingDust(BlockType block) {
        return new BlockParticleEffect(Key.key("minecraft:falling_dust"), block);
    }

    public static BlockParticleEffect dustPillar(BlockType block) {
        return new BlockParticleEffect(Key.key("minecraft:dust_pillar"), block);
    }

    public static BlockParticleEffect blockCrumble(BlockType block) {
        return new BlockParticleEffect(Key.key("minecraft:block_crumble"), block);
    }

    public static DustParticleEffect dust(ParticleColor color, float scale) {
        return new DustParticleEffect(color, scale);
    }

    public static DustTransitionParticleEffect dustTransition(
            ParticleColor fromColor,
            ParticleColor toColor,
            float scale) {
        return new DustTransitionParticleEffect(fromColor, toColor, scale);
    }

    public static ColorParticleEffect color(Key type, ParticleColor color) {
        return new ColorParticleEffect(type, color);
    }

    public static ColorParticleEffect color(ParticleKey type, ParticleColor color) {
        return color(type.key(), color);
    }

    public static ColorParticleEffect color(String type, ParticleColor color) {
        return color(Key.key(type), color);
    }

    public static SpellParticleEffect spell(Key type, ParticleColor color, float power) {
        return new SpellParticleEffect(type, color, power);
    }

    public static SpellParticleEffect spell(ParticleKey type, ParticleColor color, float power) {
        return spell(type.key(), color, power);
    }

    public static SpellParticleEffect spell(String type, ParticleColor color, float power) {
        return spell(Key.key(type), color, power);
    }

    public static PowerParticleEffect power(Key type, float power) {
        return new PowerParticleEffect(type, power);
    }

    public static PowerParticleEffect power(ParticleKey type, float power) {
        return power(type.key(), power);
    }

    public static PowerParticleEffect power(String type, float power) {
        return power(Key.key(type), power);
    }

    public static SculkChargeParticleEffect sculkCharge(float roll) {
        return new SculkChargeParticleEffect(roll);
    }

    public static ShriekParticleEffect shriek(int delay) {
        return new ShriekParticleEffect(delay);
    }

    public static TrailParticleEffect trail(Location target, ParticleColor color, int duration) {
        return new TrailParticleEffect(target, color, duration);
    }

    public static VibrationParticleEffect vibration(Location destination, int arrivalInTicks) {
        return new VibrationParticleEffect(destination, arrivalInTicks);
    }

    public static ItemParticleEffect item(ItemStack stack) {
        return new ItemParticleEffect(stack);
    }
}
