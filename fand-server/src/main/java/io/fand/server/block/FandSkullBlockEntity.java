package io.fand.server.block;

import io.fand.api.block.SkullBlockEntity;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public final class FandSkullBlockEntity extends FandBlockEntity implements SkullBlockEntity {

    public FandSkullBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.SkullBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.SkullBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.SkullBlockEntity) handle;
    }

    @Override
    public Optional<Key> noteBlockSound() {
        var sound = handle().getNoteBlockSound();
        return sound == null ? Optional.empty() : Optional.of(Key.key(sound.getNamespace(), sound.getPath()));
    }

    @Override
    public float animation() {
        return handle().getAnimation(0.0F);
    }
}
