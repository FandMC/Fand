package io.fand.server.network.packet;

import io.fand.api.packet.BlockHit;
import io.fand.api.packet.BlockPosition;
import io.fand.api.packet.Vec3d;
import io.fand.server.command.AdventureBridge;
import io.fand.server.item.FandItemStacks;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Converts vanilla packet field values to NMS-free API values and back, so the
 * dynamic {@link DynamicPacketView} can expose real fields without leaking
 * {@code net.minecraft} types into {@code fand-api}.
 *
 * <p>Conversions are by runtime type: primitives, {@code String}, {@code UUID},
 * {@code byte[]} and {@code int[]} pass through; text, item stacks, vectors and
 * block positions become their API equivalents; vanilla enums become their
 * {@code String} name; everything else is opaque (returned unchanged, only
 * readable as {@code Object}, and preserved untouched through a replace).
 */
final class PacketMarshalling {

    private static volatile @Nullable RegistryAccess registries;

    private PacketMarshalling() {
    }

    static void useRegistries(RegistryAccess access) {
        registries = access;
    }

    /** Converts a vanilla field value to its API-facing form. */
    static @Nullable Object fromVanilla(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Component component -> AdventureBridge.fromVanilla(component, registries);
            case ItemStack stack -> FandItemStacks.fromVanilla(stack);
            case Vec3 vec -> new Vec3d(vec.x, vec.y, vec.z);
            case BlockHitResult hit -> new BlockHit(
                    new BlockPosition(hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ()),
                    hit.getDirection().name(),
                    new Vec3d(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z),
                    hit.isInside());
            case BlockPos pos -> new BlockPosition(pos.getX(), pos.getY(), pos.getZ());
            case IntList ints -> ints.toIntArray();
            case Enum<?> constant -> constant.name();
            default -> value;
        };
    }

    /**
     * Converts an API value supplied through {@code with(...)} into the vanilla
     * type {@code target} expected by a record component.
     *
     * @throws IllegalArgumentException if the value cannot be marshalled to
     *         {@code target}
     */
    static @Nullable Object toVanilla(@Nullable Object value, Class<?> target) {
        if (value == null) {
            return null;
        }
        if (boxed(target).isInstance(value)) {
            return value;
        }
        if (value instanceof net.kyori.adventure.text.Component text && target == Component.class) {
            return AdventureBridge.toVanilla(text, registries);
        }
        if (value instanceof io.fand.api.item.ItemStack stack && target == ItemStack.class) {
            return FandItemStacks.toVanilla(stack);
        }
        if (value instanceof Vec3d vec && target == Vec3.class) {
            return new Vec3(vec.x(), vec.y(), vec.z());
        }
        if (value instanceof BlockPosition pos && target == BlockPos.class) {
            return new BlockPos(pos.x(), pos.y(), pos.z());
        }
        if (value instanceof String name && target.isEnum()) {
            return enumValue(target, name);
        }
        throw new IllegalArgumentException(
                "Cannot marshal " + value.getClass().getSimpleName() + " into field of type " + target.getSimpleName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> target, String name) {
        return Enum.valueOf((Class<? extends Enum>) target, name);
    }

    /** Maps a primitive class to its wrapper so {@code isInstance} works on boxed values. */
    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }
}
