package io.fand.server.entity;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.EntityEffect;
import io.fand.api.item.component.EffectKey;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EntityEffectsTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void convertsGeneratedEffectKeys() {
        var effect = new EntityEffect(EffectKey.SPEED, 100, 2, true, false, false);
        var vanilla = EntityEffects.toVanilla(effect);
        var api = EntityEffects.toApi(vanilla);

        assertThat(api.effect()).isEqualTo(EffectKey.SPEED.key());
        assertThat(api.duration()).isEqualTo(100);
        assertThat(api.amplifier()).isEqualTo(2);
        assertThat(api.ambient()).isTrue();
        assertThat(api.showParticles()).isFalse();
        assertThat(api.showIcon()).isFalse();
    }
}
