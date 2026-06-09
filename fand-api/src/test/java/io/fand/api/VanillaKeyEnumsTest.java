package io.fand.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.block.BlockEntityKey;
import io.fand.api.block.BlockKey;
import io.fand.api.entity.AttributeKey;
import io.fand.api.entity.EntityKey;
import io.fand.api.entity.VillagerProfessionKey;
import io.fand.api.inventory.MenuTypeKey;
import io.fand.api.item.ItemKey;
import io.fand.api.item.component.BannerPatternKey;
import io.fand.api.item.component.CatVariantKey;
import io.fand.api.item.component.DecoratedPotPatternKey;
import io.fand.api.item.component.EffectKey;
import io.fand.api.item.component.EnchantmentKey;
import io.fand.api.item.component.EquipmentAssetKey;
import io.fand.api.item.component.InstrumentKey;
import io.fand.api.item.component.PaintingVariantKey;
import io.fand.api.item.component.PotionKey;
import io.fand.api.item.component.TrimMaterialKey;
import io.fand.api.item.component.TrimPatternKey;
import io.fand.api.item.component.VillagerVariantKey;
import io.fand.api.item.component.WolfVariantKey;
import io.fand.api.player.StatisticKey;
import io.fand.api.recipe.RecipeIngredient;
import io.fand.api.recipe.RecipeTypeKey;
import io.fand.api.world.BiomeKey;
import io.fand.api.world.DamageTypeKey;
import io.fand.api.world.FluidKey;
import io.fand.api.world.GameEventKey;
import io.fand.api.world.GameRuleKey;
import io.fand.api.world.PoiTypeKey;
import io.fand.api.world.particle.ParticleKey;
import io.fand.api.world.sound.JukeboxSongKey;
import io.fand.api.world.sound.SoundKey;
import java.util.Arrays;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class VanillaKeyEnumsTest {

    @Test
    void generatedKeysUseMinecraftNamespace() {
        assertMinecraftNamespace(ItemKey.values());
        assertMinecraftNamespace(BlockKey.values());
        assertMinecraftNamespace(BlockEntityKey.values());
        assertMinecraftNamespace(FluidKey.values());
        assertMinecraftNamespace(EnchantmentKey.values());
        assertMinecraftNamespace(EntityKey.values());
        assertMinecraftNamespace(AttributeKey.values());
        assertMinecraftNamespace(GameEventKey.values());
        assertMinecraftNamespace(GameRuleKey.values());
        assertMinecraftNamespace(EffectKey.values());
        assertMinecraftNamespace(PotionKey.values());
        assertMinecraftNamespace(BiomeKey.values());
        assertMinecraftNamespace(DamageTypeKey.values());
        assertMinecraftNamespace(ParticleKey.values());
        assertMinecraftNamespace(SoundKey.values());
        assertMinecraftNamespace(JukeboxSongKey.values());
        assertMinecraftNamespace(InstrumentKey.values());
        assertMinecraftNamespace(EquipmentAssetKey.values());
        assertMinecraftNamespace(TrimMaterialKey.values());
        assertMinecraftNamespace(TrimPatternKey.values());
        assertMinecraftNamespace(BannerPatternKey.values());
        assertMinecraftNamespace(DecoratedPotPatternKey.values());
        assertMinecraftNamespace(VillagerVariantKey.values());
        assertMinecraftNamespace(VillagerProfessionKey.values());
        assertMinecraftNamespace(PoiTypeKey.values());
        assertMinecraftNamespace(MenuTypeKey.values());
        assertMinecraftNamespace(RecipeTypeKey.values());
        assertMinecraftNamespace(StatisticKey.values());
        assertMinecraftNamespace(WolfVariantKey.values());
        assertMinecraftNamespace(CatVariantKey.values());
        assertMinecraftNamespace(PaintingVariantKey.values());
    }

    @Test
    void generatedKeysExposeExpectedVanillaEntries() {
        assertThat(ItemKey.APPLE.key()).isEqualTo(Key.key("minecraft:apple"));
        assertThat(ItemKey.ACACIA_BOAT.key()).isEqualTo(Key.key("minecraft:acacia_boat"));
        assertThat(BlockKey.DIRT.key()).isEqualTo(Key.key("minecraft:dirt"));
        assertThat(BlockEntityKey.CHEST.key()).isEqualTo(Key.key("minecraft:chest"));
        assertThat(FluidKey.WATER.key()).isEqualTo(Key.key("minecraft:water"));
        assertThat(EnchantmentKey.SHARPNESS.key()).isEqualTo(Key.key("minecraft:sharpness"));
        assertThat(EntityKey.CREEPER.key()).isEqualTo(Key.key("minecraft:creeper"));
        assertThat(AttributeKey.ATTACK_DAMAGE.key()).isEqualTo(Key.key("minecraft:attack_damage"));
        assertThat(GameEventKey.BLOCK_PLACE.key()).isEqualTo(Key.key("minecraft:block_place"));
        assertThat(GameRuleKey.KEEP_INVENTORY.key()).isEqualTo(Key.key("minecraft:keep_inventory"));
        assertThat(EffectKey.SPEED.key()).isEqualTo(Key.key("minecraft:speed"));
        assertThat(PotionKey.WATER.key()).isEqualTo(Key.key("minecraft:water"));
        assertThat(BiomeKey.PALE_GARDEN.key()).isEqualTo(Key.key("minecraft:pale_garden"));
        assertThat(DamageTypeKey.FALL.key()).isEqualTo(Key.key("minecraft:fall"));
        assertThat(ParticleKey.TRIAL_SPAWNER_DETECTED_PLAYER.key())
                .isEqualTo(Key.key("minecraft:trial_spawner_detection"));
        assertThat(SoundKey.GOAT_HORN_SOUND_7.key()).isEqualTo(Key.key("minecraft:item.goat_horn.sound.7"));
        assertThat(SoundKey.WOLF_BIG_AMBIENT.key()).isEqualTo(Key.key("minecraft:entity.wolf_big.ambient"));
        assertThat(JukeboxSongKey.PIGSTEP.key()).isEqualTo(Key.key("minecraft:pigstep"));
        assertThat(InstrumentKey.PONDER_GOAT_HORN.key()).isEqualTo(Key.key("minecraft:ponder_goat_horn"));
        assertThat(EquipmentAssetKey.DIAMOND.key()).isEqualTo(Key.key("minecraft:diamond"));
        assertThat(TrimMaterialKey.DIAMOND.key()).isEqualTo(Key.key("minecraft:diamond"));
        assertThat(TrimPatternKey.SENTRY.key()).isEqualTo(Key.key("minecraft:sentry"));
        assertThat(BannerPatternKey.CREEPER.key()).isEqualTo(Key.key("minecraft:creeper"));
        assertThat(DecoratedPotPatternKey.ARCHER.key()).isEqualTo(Key.key("minecraft:archer"));
        assertThat(VillagerVariantKey.PLAINS.key()).isEqualTo(Key.key("minecraft:plains"));
        assertThat(VillagerProfessionKey.LIBRARIAN.key()).isEqualTo(Key.key("minecraft:librarian"));
        assertThat(PoiTypeKey.LODESTONE.key()).isEqualTo(Key.key("minecraft:lodestone"));
        assertThat(MenuTypeKey.ANVIL.key()).isEqualTo(Key.key("minecraft:anvil"));
        assertThat(RecipeTypeKey.SMELTING.key()).isEqualTo(Key.key("minecraft:smelting"));
        assertThat(StatisticKey.PLAY_TIME.key()).isEqualTo(Key.key("minecraft:play_time"));
        assertThat(WolfVariantKey.SNOWY.key()).isEqualTo(Key.key("minecraft:snowy"));
        assertThat(CatVariantKey.TABBY.key()).isEqualTo(Key.key("minecraft:tabby"));
        assertThat(PaintingVariantKey.KEBAB.key()).isEqualTo(Key.key("minecraft:kebab"));
    }

    @Test
    void generatedItemKeysCanBuildRecipeIngredients() {
        var ingredient = RecipeIngredient.of(ItemKey.DIAMOND);

        assertThat(ingredient.items()).containsExactly(ItemKey.DIAMOND.key());
        assertThat(ingredient.isTag()).isFalse();
    }

    private static void assertMinecraftNamespace(VanillaKey[] keys) {
        assertThat(Arrays.stream(keys).map(VanillaKey::asString))
                .allMatch(key -> key.startsWith("minecraft:"));
    }
}
