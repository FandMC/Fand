package io.fand.api.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.item.component.EffectKey;
import org.junit.jupiter.api.Test;

class EntityEffectTest {

    @Test
    void acceptsGeneratedEffectKeys() {
        var effect = new EntityEffect(EffectKey.SPEED, 200, 1, false, true, true);

        assertThat(effect.effect()).isEqualTo(EffectKey.SPEED.key());
        assertThat(effect.duration()).isEqualTo(200);
        assertThat(effect.amplifier()).isEqualTo(1);
    }

    @Test
    void rejectsAmplifierOutsideVanillaRange() {
        assertThatThrownBy(() -> new EntityEffect(EffectKey.SPEED, 200, 256, false, true, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
