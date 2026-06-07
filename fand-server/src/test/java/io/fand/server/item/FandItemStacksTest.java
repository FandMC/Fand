package io.fand.server.item;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import io.fand.api.item.component.CustomModelData;
import io.fand.api.item.component.EnchantmentKey;
import io.fand.api.item.component.ItemComponentKeys;
import io.fand.api.item.component.ItemRarity;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FandItemStacksTest {

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var registries = VanillaRegistries.createLookup();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
        FandItemStacks.useRegistries(registries);
    }

    @Test
    void convertsComponentsToAndFromVanillaStack() {
        var type = FandItemType.of(Items.DIAMOND);
        var customData = new JsonObject();
        customData.addProperty("source", "test");
        var api = type.one()
                .withMaxStackSize(99)
                .withAmount(99)
                .withCustomName(Component.text("Named"))
                .withLore(Component.text("line one"), Component.text("line two"))
                .withCustomModelData(new CustomModelData(List.of(7.0F), List.of(true), List.of("model"), List.of(0xFFAA00)))
                .withEnchantmentGlintOverride(true)
                .withEnchantment(EnchantmentKey.SHARPNESS, 5)
                .withStoredEnchantment(EnchantmentKey.MENDING, 1)
                .withEnchantable(30)
                .withHiddenTooltipComponent(ItemComponentKeys.STORED_ENCHANTMENTS, true)
                .withRarity(ItemRarity.EPIC)
                .withCustomData(customData);

        var vanilla = FandItemStacks.toVanilla(api);
        var roundTripped = FandItemStacks.fromVanilla(vanilla);

        assertThat(vanilla.getCount()).isEqualTo(99);
        assertThat(vanilla.getMaxStackSize()).isEqualTo(99);
        assertThat(roundTripped.amount()).isEqualTo(99);
        assertThat(roundTripped.maxStackSize()).isEqualTo(99);
        assertThat(roundTripped.customName()).contains(Component.text("Named"));
        assertThat(roundTripped.lore()).containsExactly(Component.text("line one"), Component.text("line two"));
        assertThat(roundTripped.customModelData()).contains(new CustomModelData(List.of(7.0F), List.of(true), List.of("model"), List.of(0xFFAA00)));
        assertThat(roundTripped.enchantmentGlintOverride()).contains(true);
        assertThat(roundTripped.enchantments().level(EnchantmentKey.SHARPNESS)).isEqualTo(5);
        assertThat(roundTripped.storedEnchantments().level(EnchantmentKey.MENDING)).isEqualTo(1);
        assertThat(roundTripped.enchantable()).contains(30);
        assertThat(roundTripped.tooltipDisplay().hides(ItemComponentKeys.STORED_ENCHANTMENTS)).isTrue();
        assertThat(roundTripped.rarity()).contains(ItemRarity.EPIC);
        assertThat(roundTripped.customData()).get().extracting(json -> json.get("source").getAsString()).isEqualTo("test");
    }
}
