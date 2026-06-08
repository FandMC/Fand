package io.fand.api.entity;

import io.fand.api.item.component.EffectKey;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Active mob-effect instance on a living entity. */
public record EntityEffect(
        Key effect,
        int duration,
        int amplifier,
        boolean ambient,
        boolean showParticles,
        boolean showIcon) {

    public EntityEffect {
        effect = Objects.requireNonNull(effect, "effect");
        if (amplifier < 0 || amplifier > 255) {
            throw new IllegalArgumentException("amplifier must be in 0..255");
        }
    }

    public EntityEffect(Key effect, int duration) {
        this(effect, duration, 0, false, true, true);
    }

    public EntityEffect(EffectKey effect, int duration) {
        this(Objects.requireNonNull(effect, "effect").key(), duration);
    }

    public EntityEffect(
            EffectKey effect,
            int duration,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon) {
        this(Objects.requireNonNull(effect, "effect").key(), duration, amplifier, ambient, showParticles, showIcon);
    }
}
