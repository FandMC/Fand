package io.fand.api.item.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class ItemEnchantmentsTest {

    @Test
    void writesAndReadsLevels() {
        var enchantments = ItemEnchantments.empty()
                .with(EnchantmentKeys.SHARPNESS, 3)
                .upgrade(EnchantmentKeys.SHARPNESS, 5)
                .with(Key.key("custom:glow"), 1);

        var roundTripped = ItemEnchantments.fromJson(enchantments.toJson());

        assertThat(roundTripped.level(EnchantmentKeys.SHARPNESS)).isEqualTo(5);
        assertThat(roundTripped.level(Key.key("custom:glow"))).isEqualTo(1);
        assertThat(roundTripped.has(EnchantmentKeys.MENDING)).isFalse();
    }

    @Test
    void nonPositiveLevelsRemoveEntries() {
        var enchantments = ItemEnchantments.of(EnchantmentKeys.UNBREAKING, 3)
                .with(EnchantmentKeys.UNBREAKING, 0);

        assertThat(enchantments.isEmpty()).isTrue();
    }

    @Test
    void rejectsVanillaOutOfRangeLevels() {
        assertThatThrownBy(() -> ItemEnchantments.of(EnchantmentKeys.SHARPNESS, 256))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1..255");
    }

    @Test
    void ignoresZeroLevelsFromJson() {
        var json = new JsonObject();
        json.addProperty(EnchantmentKeys.SHARPNESS.asString(), 0);

        assertThat(ItemEnchantments.fromJson(json).isEmpty()).isTrue();
    }
}
