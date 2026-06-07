package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.item.component.ItemRarity;
import io.fand.api.world.particle.ParticleColor;
import io.fand.api.world.particle.Particles;
import io.fand.server.block.FandBlockType;
import io.fand.server.item.FandItemStacks;
import io.fand.server.item.FandItemType;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ParticleEffectsTest {

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var registries = VanillaRegistries.createLookup();
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
        FandItemStacks.useRegistries(registries);
    }

    @Test
    void convertsSimpleParticles() {
        var particle = ParticleEffects.toVanilla(Particles.simple("minecraft:happy_villager"));

        assertThat(particle).isInstanceOf(SimpleParticleType.class);
        assertThat(particle).isSameAs(ParticleTypes.HAPPY_VILLAGER);
    }

    @Test
    void convertsBlockParticles() {
        var particle = ParticleEffects.toVanilla(Particles.block(FandBlockType.of(Blocks.STONE)));

        assertThat(particle).isInstanceOf(BlockParticleOption.class);
        assertThat(((BlockParticleOption) particle).getState().getBlock()).isSameAs(Blocks.STONE);
    }

    @Test
    void convertsColoredParticles() {
        var dust = ParticleEffects.toVanilla(Particles.dust(ParticleColor.rgb(0x33CCFF), 1.5F));
        var color = ParticleEffects.toVanilla(Particles.color("minecraft:entity_effect", ParticleColor.rgba(51, 204, 255, 128)));

        assertThat(dust).isInstanceOf(DustParticleOptions.class);
        assertThat(((DustParticleOptions) dust).getScale()).isEqualTo(1.5F);
        assertThat(color).isInstanceOf(ColorParticleOption.class);
        assertThat(((ColorParticleOption) color).getAlpha()).isCloseTo(128.0F / 255.0F, withinFloat(0.0001F));
        assertThat(((ColorParticleOption) color).getRed()).isCloseTo(51.0F / 255.0F, withinFloat(0.0001F));
    }

    @Test
    void convertsItemParticlesWithComponents() {
        var item = FandItemType.of(Items.DIAMOND).one()
                .withCustomName(Component.text("Particle Diamond"))
                .withRarity(ItemRarity.RARE);

        var particle = ParticleEffects.toVanilla(Particles.item(item));

        assertThat(particle).isInstanceOf(ItemParticleOption.class);
        var template = ((ItemParticleOption) particle).getItem();
        assertThat(template.item().value()).isSameAs(Items.DIAMOND);
        assertThat(template.components().isEmpty()).isFalse();
        assertThat(template.get(DataComponents.CUSTOM_NAME)).isNotNull();
    }

    @Test
    void rejectsMismatchedParticlePayloadTypes() {
        assertThatThrownBy(() -> ParticleEffects.toVanilla(Particles.simple("minecraft:item")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty payload");
        assertThatThrownBy(() -> ParticleEffects.toVanilla(Particles.color("minecraft:flame", ParticleColor.rgb(0xFFFFFF))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("color payload");
    }

    private static org.assertj.core.data.Offset<Float> withinFloat(float offset) {
        return org.assertj.core.data.Offset.offset(offset);
    }
}
