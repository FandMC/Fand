package io.fand.api.world.particle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class ParticleModelsTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);

    @Test
    void createsParticleColorsFromRgbAndRgba() {
        var rgb = ParticleColor.rgb(0x33CCFF);
        var rgba = ParticleColor.rgba(0x33, 0xCC, 0xFF, 0x80);

        assertThat(rgb.argb()).isEqualTo(0xFF33CCFF);
        assertThat(rgb.rgb()).isEqualTo(0x33CCFF);
        assertThat(rgba.alpha()).isEqualTo(0x80);
        assertThat(rgba.red()).isEqualTo(0x33);
        assertThat(rgba.green()).isEqualTo(0xCC);
        assertThat(rgba.blue()).isEqualTo(0xFF);
    }

    @Test
    void rejectsInvalidParticleColors() {
        assertThatThrownBy(() -> ParticleColor.rgb(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rgb");
        assertThatThrownBy(() -> ParticleColor.rgba(0, 0, 0, 256))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alpha");
    }

    @Test
    void validatesParticleEmission() {
        var emission = ParticleEmission.count(8)
                .withOffset(0.25, 0.5, 0.25)
                .withSpeed(0.1)
                .withAlwaysShow(true);

        assertThat(emission.count()).isEqualTo(8);
        assertThat(emission.offsetY()).isEqualTo(0.5);
        assertThat(emission.speed()).isEqualTo(0.1);
        assertThat(emission.alwaysShow()).isTrue();
        assertThatThrownBy(() -> ParticleEmission.count(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count");
        assertThatThrownBy(() -> ParticleEmission.SINGLE.withSpeed(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("speed");
    }

    @Test
    void validatesPayloadSpecificParticleModels() {
        assertThat(Particles.simple(ParticleKey.HEART).type()).isEqualTo(ParticleKey.HEART.key());
        assertThat(Particles.simple("minecraft:happy_villager").type())
                .isEqualTo(Key.key("minecraft:happy_villager"));
        assertThat(Particles.color(ParticleKey.ENTITY_EFFECT, ParticleColor.rgb(0x224466)).type())
                .isEqualTo(ParticleKey.ENTITY_EFFECT.key());
        assertThat(Particles.item(DIAMOND.one()).stack().type()).isEqualTo(DIAMOND);
        assertThat(Particles.dust(ParticleColor.rgb(0xAA0000), 1.5F).scale()).isEqualTo(1.5F);
        assertThat(Particles.shriek(4).delay()).isEqualTo(4);

        assertThatThrownBy(() -> Particles.item(ItemStack.EMPTY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-empty");
        assertThatThrownBy(() -> Particles.dust(ParticleColor.rgb(0), 0.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scale");
        assertThatThrownBy(() -> Particles.shriek(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delay");
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
