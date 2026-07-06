package io.fand.api.world.particle;

import io.fand.api.item.ItemStack;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Item particle data backed by a full Fand {@link ItemStack}, including data components. */
public record ItemParticleEffect(ItemStack stack) implements ParticleEffect {

    private static final Key TYPE = Key.key("minecraft:item");

    public ItemParticleEffect {
        Objects.requireNonNull(stack, "stack");
        if (stack.empty()) {
            throw new IllegalArgumentException("stack must be non-empty");
        }
    }

    @Override
    public Key type() {
        return TYPE;
    }
}
