package io.fand.api.block;

import net.kyori.adventure.key.Key;

/** Common vanilla fluid keys and lightweight lookup helpers. */
public final class FluidTypes {

    public static final FluidType EMPTY = of(Key.key("minecraft:empty"));
    public static final FluidType WATER = of(Key.key("minecraft:water"));
    public static final FluidType FLOWING_WATER = of(Key.key("minecraft:flowing_water"));
    public static final FluidType LAVA = of(Key.key("minecraft:lava"));
    public static final FluidType FLOWING_LAVA = of(Key.key("minecraft:flowing_lava"));

    private FluidTypes() {
    }

    public static FluidType of(Key key) {
        java.util.Objects.requireNonNull(key, "key");
        return new KeyedFluidType(key);
    }

    public static FluidType water(boolean source) {
        return source ? WATER : FLOWING_WATER;
    }

    public static FluidType lava(boolean source) {
        return source ? LAVA : FLOWING_LAVA;
    }

    private record KeyedFluidType(Key key) implements FluidType {
    }
}
