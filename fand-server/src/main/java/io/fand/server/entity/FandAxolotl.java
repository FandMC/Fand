package io.fand.server.entity;

import io.fand.api.entity.Axolotl;
import io.fand.server.util.ReflectionFields;
import io.fand.server.world.WorldRegistry;
import java.lang.reflect.Method;
import java.util.Objects;

public final class FandAxolotl extends FandAnimal implements Axolotl {
    private static final Method SET_VARIANT = ReflectionFields.method(
            net.minecraft.world.entity.animal.axolotl.Axolotl.class,
            "setVariant",
            net.minecraft.world.entity.animal.axolotl.Axolotl.Variant.class);

    public FandAxolotl(net.minecraft.world.entity.animal.axolotl.Axolotl handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.axolotl.Axolotl handle() {
        return (net.minecraft.world.entity.animal.axolotl.Axolotl) handle;
    }

    @Override
    public Variant variant() {
        return Variant.valueOf(handle().getVariant().name());
    }

    @Override
    public void setVariant(Variant variant) {
        Objects.requireNonNull(variant, "variant");
        var vanilla = net.minecraft.world.entity.animal.axolotl.Axolotl.Variant.valueOf(variant.name());
        runOnServerThread(() -> ReflectionFields.invoke(SET_VARIANT, handle(), vanilla));
    }

    @Override
    public boolean playingDead() {
        return handle().isPlayingDead();
    }

    @Override
    public void setPlayingDead(boolean playingDead) {
        runOnServerThread(() -> handle().setPlayingDead(playingDead));
    }

    @Override
    public boolean fromBucket() {
        return handle().fromBucket();
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        runOnServerThread(() -> handle().setFromBucket(fromBucket));
    }
}
