package io.fand.server.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Wolf;
import io.fand.api.item.component.ItemDyeColor;
import io.fand.server.util.ReflectionFields;
import io.fand.server.world.WorldRegistry;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

public final class FandWolf extends FandTameable implements Wolf {
    private static final Method GET_VARIANT = ReflectionFields.method(
            net.minecraft.world.entity.animal.wolf.Wolf.class,
            "getVariant");
    private static final Method SET_VARIANT = ReflectionFields.method(
            net.minecraft.world.entity.animal.wolf.Wolf.class,
            "setVariant",
            Holder.class);
    private static final Method GET_SOUND_VARIANT = ReflectionFields.method(
            net.minecraft.world.entity.animal.wolf.Wolf.class,
            "getSoundVariant");
    private static final Method SET_SOUND_VARIANT = ReflectionFields.method(
            net.minecraft.world.entity.animal.wolf.Wolf.class,
            "setSoundVariant",
            Holder.class);
    private static final Method SET_COLLAR_COLOR = ReflectionFields.method(
            net.minecraft.world.entity.animal.wolf.Wolf.class,
            "setCollarColor",
            DyeColor.class);

    public FandWolf(net.minecraft.world.entity.animal.wolf.Wolf handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.wolf.Wolf handle() {
        return (net.minecraft.world.entity.animal.wolf.Wolf) handle;
    }

    @Override
    public boolean angry() {
        return FandAngerable.angry(handle());
    }

    @Override
    public long angerEndTime() {
        return FandAngerable.angerEndTime(handle());
    }

    @Override
    public void setAngerEndTime(long gameTime) {
        runOnServerThread(() -> FandAngerable.setAngerEndTime(handle(), gameTime));
    }

    @Override
    public void startAngerTimer() {
        runOnServerThread(() -> FandAngerable.startAngerTimer(handle()));
    }

    @Override
    public Optional<UUID> angerTargetId() {
        return FandAngerable.angerTargetId(handle());
    }

    @Override
    public void setAngerTarget(@Nullable LivingEntity target) {
        runOnServerThread(() -> FandAngerable.setAngerTarget(handle(), target));
    }

    @Override
    public void clearAnger() {
        runOnServerThread(() -> FandAngerable.clearAnger(handle()));
    }

    @Override
    public Key variant() {
        var holder = ReflectionFields.call(GET_VARIANT, handle(), Holder.class);
        return apiKey(((Holder<?>) holder).unwrapKey().orElseThrow().identifier());
    }

    @Override
    public void setVariant(Key variant) {
        Objects.requireNonNull(variant, "variant");
        runOnServerThread(() -> ReflectionFields.invoke(SET_VARIANT, handle(), wolfVariant(variant)));
    }

    @Override
    public Key soundVariant() {
        var holder = ReflectionFields.call(GET_SOUND_VARIANT, handle(), Holder.class);
        return apiKey(((Holder<?>) holder).unwrapKey().orElseThrow().identifier());
    }

    @Override
    public void setSoundVariant(Key variant) {
        Objects.requireNonNull(variant, "variant");
        runOnServerThread(() -> ReflectionFields.invoke(SET_SOUND_VARIANT, handle(), wolfSoundVariant(variant)));
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
    public boolean interested() {
        return handle().isInterested();
    }

    @Override
    public void setInterested(boolean interested) {
        runOnServerThread(() -> handle().setIsInterested(interested));
    }

    private Holder<WolfVariant> wolfVariant(Key key) {
        return handle().registryAccess()
                .lookupOrThrow(Registries.WOLF_VARIANT)
                .get(ResourceKey.create(Registries.WOLF_VARIANT, id(key)))
                .orElseThrow(() -> new IllegalArgumentException("Unknown wolf variant: " + key.asString()));
    }

    private Holder<WolfSoundVariant> wolfSoundVariant(Key key) {
        return handle().registryAccess()
                .lookupOrThrow(Registries.WOLF_SOUND_VARIANT)
                .get(ResourceKey.create(Registries.WOLF_SOUND_VARIANT, id(key)))
                .orElseThrow(() -> new IllegalArgumentException("Unknown wolf sound variant: " + key.asString()));
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
