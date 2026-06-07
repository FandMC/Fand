package io.fand.server.world;

import io.fand.api.block.BlockType;
import io.fand.api.world.Location;
import io.fand.api.world.particle.BlockParticleEffect;
import io.fand.api.world.particle.ColorParticleEffect;
import io.fand.api.world.particle.DustParticleEffect;
import io.fand.api.world.particle.DustTransitionParticleEffect;
import io.fand.api.world.particle.ItemParticleEffect;
import io.fand.api.world.particle.ParticleEffect;
import io.fand.api.world.particle.ParticleEmission;
import io.fand.api.world.particle.PowerParticleEffect;
import io.fand.api.world.particle.SculkChargeParticleEffect;
import io.fand.api.world.particle.ShriekParticleEffect;
import io.fand.api.world.particle.SimpleParticleEffect;
import io.fand.api.world.particle.SpellParticleEffect;
import io.fand.api.world.particle.TrailParticleEffect;
import io.fand.api.world.particle.VibrationParticleEffect;
import io.fand.server.block.FandBlockType;
import io.fand.server.item.FandItemStacks;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.phys.Vec3;

public final class ParticleEffects {

    private ParticleEffects() {
    }

    public static void spawn(ServerLevel level, Location location, ParticleEffect effect, ParticleEmission emission) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(emission, "emission");
        level.sendParticles(
                toVanilla(effect),
                emission.overrideLimiter(),
                emission.alwaysShow(),
                location.x(),
                location.y(),
                location.z(),
                emission.count(),
                emission.offsetX(),
                emission.offsetY(),
                emission.offsetZ(),
                emission.speed());
    }

    public static void spawnTo(ServerPlayer player, Location location, ParticleEffect effect, ParticleEmission emission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(emission, "emission");
        player.level().sendParticles(
                player,
                toVanilla(effect),
                emission.overrideLimiter(),
                emission.alwaysShow(),
                location.x(),
                location.y(),
                location.z(),
                emission.count(),
                emission.offsetX(),
                emission.offsetY(),
                emission.offsetZ(),
                emission.speed());
    }

    public static ParticleOptions toVanilla(ParticleEffect effect) {
        Objects.requireNonNull(effect, "effect");
        if (effect instanceof SimpleParticleEffect simple) {
            return simpleType(simple.type());
        }
        if (effect instanceof BlockParticleEffect block) {
            return new BlockParticleOption(blockType(block.type()), block(block.block()).handle().defaultBlockState());
        }
        if (effect instanceof DustParticleEffect dust) {
            return new DustParticleOptions(dust.color().rgb(), dust.scale());
        }
        if (effect instanceof DustTransitionParticleEffect dust) {
            return new DustColorTransitionOptions(dust.fromColor().rgb(), dust.toColor().rgb(), dust.scale());
        }
        if (effect instanceof ColorParticleEffect color) {
            return ColorParticleOption.create(colorType(color.type()), color.color().argb());
        }
        if (effect instanceof SpellParticleEffect spell) {
            return SpellParticleOption.create(spellType(spell.type()), spell.color().rgb(), spell.power());
        }
        if (effect instanceof PowerParticleEffect power) {
            return PowerParticleOption.create(powerType(power.type()), power.power());
        }
        if (effect instanceof SculkChargeParticleEffect sculk) {
            return new SculkChargeParticleOptions(sculk.roll());
        }
        if (effect instanceof ShriekParticleEffect shriek) {
            return new ShriekParticleOption(shriek.delay());
        }
        if (effect instanceof TrailParticleEffect trail) {
            return new TrailParticleOption(toVec3(trail.target()), trail.color().rgb(), trail.duration());
        }
        if (effect instanceof VibrationParticleEffect vibration) {
            return new VibrationParticleOption(
                    new BlockPositionSource(toBlockPos(vibration.destination())),
                    vibration.arrivalInTicks());
        }
        if (effect instanceof ItemParticleEffect item) {
            var vanilla = FandItemStacks.toVanilla(item.stack());
            return new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(vanilla));
        }
        throw new IllegalArgumentException("Unsupported particle effect: " + effect.getClass().getName());
    }

    static SimpleParticleType simpleType(Key key) {
        var type = particleType(key);
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        throw new IllegalArgumentException("Particle " + key.asString() + " does not accept an empty payload");
    }

    static ParticleType<BlockParticleOption> blockType(Key key) {
        var type = particleType(key);
        if (type == ParticleTypes.BLOCK
                || type == ParticleTypes.BLOCK_MARKER
                || type == ParticleTypes.FALLING_DUST
                || type == ParticleTypes.DUST_PILLAR
                || type == ParticleTypes.BLOCK_CRUMBLE) {
            return cast(type);
        }
        throw new IllegalArgumentException("Particle " + key.asString() + " does not accept a block payload");
    }

    static ParticleType<ColorParticleOption> colorType(Key key) {
        var type = particleType(key);
        if (type == ParticleTypes.ENTITY_EFFECT
                || type == ParticleTypes.TINTED_LEAVES
                || type == ParticleTypes.FLASH) {
            return cast(type);
        }
        throw new IllegalArgumentException("Particle " + key.asString() + " does not accept a color payload");
    }

    static ParticleType<SpellParticleOption> spellType(Key key) {
        var type = particleType(key);
        if (type == ParticleTypes.EFFECT || type == ParticleTypes.INSTANT_EFFECT) {
            return cast(type);
        }
        throw new IllegalArgumentException("Particle " + key.asString() + " does not accept a spell payload");
    }

    static ParticleType<PowerParticleOption> powerType(Key key) {
        var type = particleType(key);
        if (type == ParticleTypes.DRAGON_BREATH) {
            return cast(type);
        }
        throw new IllegalArgumentException("Particle " + key.asString() + " does not accept a power payload");
    }

    private static ParticleType<?> particleType(Key key) {
        return BuiltInRegistries.PARTICLE_TYPE.getOptional(id(key))
                .orElseThrow(() -> new IllegalArgumentException("Unknown particle type: " + key.asString()));
    }

    private static FandBlockType block(BlockType type) {
        if (type instanceof FandBlockType fand) {
            return fand;
        }
        throw new IllegalArgumentException("BlockType must be obtained from BlockTypes / Server.blockType");
    }

    private static Vec3 toVec3(Location location) {
        return new Vec3(location.x(), location.y(), location.z());
    }

    private static BlockPos toBlockPos(Location location) {
        return new BlockPos(location.blockX(), location.blockY(), location.blockZ());
    }

    private static Identifier id(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    @SuppressWarnings("unchecked")
    private static <T extends ParticleOptions> ParticleType<T> cast(ParticleType<?> type) {
        return (ParticleType<T>) type;
    }
}
