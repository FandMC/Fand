package io.fand.server.entity;

import io.fand.api.entity.Cat;
import io.fand.api.item.component.ItemDyeColor;
import io.fand.server.util.ReflectionFields;
import io.fand.server.world.WorldRegistry;
import java.lang.reflect.Method;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.animal.feline.CatSoundVariant;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.item.DyeColor;

public final class FandCat extends FandTameable implements Cat {
    private static final Method SET_VARIANT = ReflectionFields.method(
            net.minecraft.world.entity.animal.feline.Cat.class,
            "setVariant",
            Holder.class);
    private static final Method GET_SOUND_VARIANT = ReflectionFields.method(
            net.minecraft.world.entity.animal.feline.Cat.class,
            "getSoundVariant");
    private static final Method SET_SOUND_VARIANT = ReflectionFields.method(
            net.minecraft.world.entity.animal.feline.Cat.class,
            "setSoundVariant",
            Holder.class);
    private static final Method SET_RELAX_STATE_ONE = ReflectionFields.method(
            net.minecraft.world.entity.animal.feline.Cat.class,
            "setRelaxStateOne",
            boolean.class);
    private static final Method IS_RELAX_STATE_ONE = ReflectionFields.method(
            net.minecraft.world.entity.animal.feline.Cat.class,
            "isRelaxStateOne");
    private static final Method SET_COLLAR_COLOR = ReflectionFields.method(
            net.minecraft.world.entity.animal.feline.Cat.class,
            "setCollarColor",
            DyeColor.class);

    public FandCat(net.minecraft.world.entity.animal.feline.Cat handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.feline.Cat handle() {
        return (net.minecraft.world.entity.animal.feline.Cat) handle;
    }

    @Override
    public Key variant() {
        return apiKey(handle().getVariant().unwrapKey().orElseThrow().identifier());
    }

    @Override
    public void setVariant(Key variant) {
        Objects.requireNonNull(variant, "variant");
        runOnServerThread(() -> ReflectionFields.invoke(SET_VARIANT, handle(), catVariant(variant)));
    }

    @Override
    public Key soundVariant() {
        var holder = ReflectionFields.call(GET_SOUND_VARIANT, handle(), Holder.class);
        return apiKey(((Holder<?>) holder).unwrapKey().orElseThrow().identifier());
    }

    @Override
    public void setSoundVariant(Key variant) {
        Objects.requireNonNull(variant, "variant");
        runOnServerThread(() -> ReflectionFields.invoke(SET_SOUND_VARIANT, handle(), catSoundVariant(variant)));
    }

    @Override
    public ItemDyeColor collarColor() {
        return ItemDyeColor.fromSerializedName(handle().getCollarColor().getName());
    }

    @Override
    public void setCollarColor(ItemDyeColor color) {
        Objects.requireNonNull(color, "color");
        runOnServerThread(() -> ReflectionFields.invoke(SET_COLLAR_COLOR, handle(), dyeColor(color)));
    }

    @Override
    public boolean lying() {
        return handle().isLying();
    }

    @Override
    public void setLying(boolean lying) {
        runOnServerThread(() -> handle().setLying(lying));
    }

    @Override
    public boolean relaxed() {
        return ReflectionFields.booleanValue(IS_RELAX_STATE_ONE, handle());
    }

    @Override
    public void setRelaxed(boolean relaxed) {
        runOnServerThread(() -> ReflectionFields.invoke(SET_RELAX_STATE_ONE, handle(), relaxed));
    }

    @Override
    public boolean lyingOnSleepingPlayer() {
        return handle().isLyingOnTopOfSleepingPlayer();
    }

    private Holder<CatVariant> catVariant(Key key) {
        return handle().registryAccess()
                .lookupOrThrow(Registries.CAT_VARIANT)
                .get(ResourceKey.create(Registries.CAT_VARIANT, id(key)))
                .orElseThrow(() -> new IllegalArgumentException("Unknown cat variant: " + key.asString()));
    }

    private Holder<CatSoundVariant> catSoundVariant(Key key) {
        return handle().registryAccess()
                .lookupOrThrow(Registries.CAT_SOUND_VARIANT)
                .get(ResourceKey.create(Registries.CAT_SOUND_VARIANT, id(key)))
                .orElseThrow(() -> new IllegalArgumentException("Unknown cat sound variant: " + key.asString()));
    }

    private static DyeColor dyeColor(ItemDyeColor color) {
        var dye = DyeColor.byName(color.serializedName(), null);
        if (dye == null) {
            throw new IllegalArgumentException("Unknown dye color: " + color);
        }
        return dye;
    }

    private static Identifier id(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static Key apiKey(Identifier id) {
        return Key.key(id.getNamespace(), id.getPath());
    }
}
