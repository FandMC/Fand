package io.fand.api.item.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class ItemEnchantmentsTest {

    @Test
    void writesAndReadsLevels() {
        var enchantments = ItemEnchantments.emptyEnchantments()
                .with(EnchantmentKey.SHARPNESS, 3)
                .upgrade(EnchantmentKey.SHARPNESS, 5)
                .with(Key.key("custom:glow"), 1);

        var roundTripped = ItemEnchantments.fromJson(enchantments.toJson());

        assertThat(roundTripped.level(EnchantmentKey.SHARPNESS)).isEqualTo(5);
        assertThat(roundTripped.level(Key.key("custom:glow"))).isEqualTo(1);
        assertThat(roundTripped.contains(EnchantmentKey.MENDING)).isFalse();
    }

    @Test
    void nonPositiveLevelsRemoveEntries() {
        var enchantments = ItemEnchantments.of(EnchantmentKey.UNBREAKING, 3)
                .with(EnchantmentKey.UNBREAKING, 0);

        assertThat(enchantments.empty()).isTrue();
    }

    @Test
    void rejectsVanillaOutOfRangeLevels() {
        assertThatThrownBy(() -> ItemEnchantments.of(EnchantmentKey.SHARPNESS, 256))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1..255");
    }

    @Test
    void ignoresZeroLevelsFromJson() {
        var json = new JsonObject();
        json.addProperty(EnchantmentKey.SHARPNESS.asString(), 0);

        assertThat(ItemEnchantments.fromJson(json).empty()).isTrue();
    }
}
