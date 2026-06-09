package io.fand.server.block;

import io.fand.api.block.BeaconBlockEntity;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.LoggerFactory;

public final class FandBeaconBlockEntity extends FandBlockEntity implements BeaconBlockEntity {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FandBeaconBlockEntity.class);

    public FandBeaconBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.BeaconBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.BeaconBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.BeaconBlockEntity) handle;
    }

    @Override
    public int levels() {
        return data().getIntOr("Levels", 0);
    }

    @Override
    public Optional<Key> primaryEffect() {
        return effect("primary_effect");
    }

    @Override
    public boolean setPrimaryEffect(Key effect) {
        return setEffect("primary_effect", effect);
    }

    @Override
    public void clearPrimaryEffect() {
        clearEffect("primary_effect");
    }

    @Override
    public Optional<Key> secondaryEffect() {
        return effect("secondary_effect");
    }

    @Override
    public boolean setSecondaryEffect(Key effect) {
        return setEffect("secondary_effect", effect);
    }

    @Override
    public void clearSecondaryEffect() {
        clearEffect("secondary_effect");
    }

    private Optional<Key> effect(String field) {
        return data().getString(field).map(Key::key);
    }

    private boolean setEffect(String field, Key effect) {
        Objects.requireNonNull(effect, "effect");
        var holder = BuiltInRegistries.MOB_EFFECT.get(Identifier.fromNamespaceAndPath(effect.namespace(), effect.value()));
        if (holder.isEmpty() || !validEffect(holder.orElseThrow().value())) {
            return false;
        }
        block.runOnServerThread(() -> {
            var tag = data();
            tag.putString(field, effect.asString());
            load(tag);
        });
        return true;
    }

    private void clearEffect(String field) {
        block.runOnServerThread(() -> {
            var tag = data();
            tag.remove(field);
            load(tag);
        });
    }

    private CompoundTag data() {
        return handle().saveCustomOnly(block.worldHandle().registryAccess());
    }

    private void load(CompoundTag tag) {
        try (var reporter = new ProblemReporter.ScopedCollector(handle().problemPath(), LOGGER)) {
            handle().loadCustomOnly(TagValueInput.create(reporter, block.worldHandle().registryAccess(), tag));
        }
        handle().setChanged();
    }

    private static boolean validEffect(MobEffect effect) {
        return net.minecraft.world.level.block.entity.BeaconBlockEntity.BEACON_EFFECTS.stream()
                .flatMap(java.util.Collection::stream)
                .anyMatch(holder -> holder.value() == effect);
    }
}
