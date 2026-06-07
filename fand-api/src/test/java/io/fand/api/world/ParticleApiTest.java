package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.block.BlockType;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import java.lang.reflect.Modifier;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class ParticleApiTest {

    @Test
    void commonParticleConstantsAreFinalApiFields() {
        for (var field : Particles.class.getFields()) {
            if (field.getType() == Particle.class) {
                assertThat(Modifier.isFinal(field.getModifiers()))
                        .as(field.getName())
                        .isTrue();
            }
        }
    }

    @Test
    void keyFactoryAndRawEscapeHatchAreExplicitAndCached() {
        var key = Key.key("minecraft:flame");
        var raw = "minecraft:dust{color:16711680,scale:1.0}";

        assertThat(Particles.particle(key)).isSameAs(Particles.FLAME);
        assertThat(Particles.raw(raw)).isSameAs(Particles.raw(raw));
        assertThat(Particles.raw(raw).key()).isEqualTo(Key.key("minecraft:dust"));
    }

    @Test
    void typedBlockAndItemHelpersBuildVanillaArguments() {
        assertThat(Particles.block(new TestBlockType(Key.key("minecraft:stone"))).argument())
                .isEqualTo("minecraft:block{block_state:\"minecraft:stone\"}");
        assertThat(Particles.fallingDust(new TestBlockType(Key.key("minecraft:sand"))).argument())
                .isEqualTo("minecraft:falling_dust{block_state:\"minecraft:sand\"}");
        assertThat(Particles.item(new TestItemType(Key.key("minecraft:diamond"))).argument())
                .isEqualTo("minecraft:item{item:{id:\"minecraft:diamond\"}}");
    }

    @Test
    void rejectsInvalidParticleBuilderParameters() {
        assertThatThrownBy(() -> Particles.dust(-1, 1.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rgb must be in");
        assertThatThrownBy(() -> Particles.dust(0x1000000, 1.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rgb must be in");
        assertThatThrownBy(() -> Particles.dust(0xFFFFFF, Float.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scale must be finite");
        assertThatThrownBy(() -> Particles.dust(1.1F, 0.0F, 0.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("red must be in [0, 1]");
        assertThatThrownBy(() -> Particles.shriek(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delay must be >= 0");
        assertThatThrownBy(() -> Particles.sculkCharge(Float.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roll must be finite");
    }

    @Test
    void rejectsNonFinitePlaybackParameters() {
        assertThatThrownBy(() -> ParticlePlayback.of(Particles.FLAME, Double.NaN, 0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("x must be finite");
        assertThatThrownBy(() -> Particles.FLAME.at(0.0, 0.0, 0.0).offset(0.0, Double.POSITIVE_INFINITY, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offsetY must be finite");
        assertThatThrownBy(() -> Particles.FLAME.at(0.0, 0.0, 0.0).speed(Double.NEGATIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("speed must be finite");
        assertThatThrownBy(() -> Particles.FLAME.at(Double.MAX_VALUE, 0.0, 0.0).move(Double.MAX_VALUE, 0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("x must be finite");
    }

    private record TestBlockType(Key key) implements BlockType {
    }

    private record TestItemType(Key key) implements ItemType {
        @Override
        public int maxStackSize() {
            return 64;
        }

        @Override
        public ItemStack stack(int amount) {
            return ItemType.super.stack(amount);
        }
    }
}
