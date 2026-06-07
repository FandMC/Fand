package io.fand.api.world.particle;

import io.fand.api.block.BlockType;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** A particle carrying a block state, such as {@code block} or {@code falling_dust}. */
public record BlockParticleEffect(Key type, BlockType block) implements ParticleEffect {

    public BlockParticleEffect {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(block, "block");
    }
}
