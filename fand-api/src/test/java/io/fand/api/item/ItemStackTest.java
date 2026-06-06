package io.fand.api.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import io.fand.api.item.component.CustomModelData;
import io.fand.api.item.component.ItemComponentKeys;
import io.fand.api.item.component.ItemComponents;
import io.fand.api.item.component.ItemRarity;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class ItemStackTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);

    @Test
    void keepsComponentsImmutable() {
        var stack = new ItemStack(DIAMOND, 1)
                .withCustomName(Component.text("Named"));

        var changed = stack.withoutCustomName();

        assertThat(stack.customName()).contains(Component.text("Named"));
        assertThat(changed.customName()).isEmpty();
    }

    @Test
    void readsAndWritesCommonComponents() {
        var customData = new JsonObject();
        customData.addProperty("owner", "test");

        var stack = new ItemStack(DIAMOND, 1)
                .withCustomName(Component.text("Custom"))
                .withItemName(Component.text("Item"))
                .withLore(Component.text("one"), Component.text("two"))
                .withItemModel(Key.key("fand:test_model"))
                .withCustomModelData(new CustomModelData(List.of(1.0F), List.of(true), List.of("demo"), List.of(0x336699)))
                .withEnchantmentGlintOverride(true)
                .withUnbreakable(true)
                .withDamage(3)
                .withMaxDamage(10)
                .withRepairCost(2)
                .withRarity(ItemRarity.RARE)
                .withCustomData(customData);

        assertThat(stack.customName()).contains(Component.text("Custom"));
        assertThat(stack.itemName()).contains(Component.text("Item"));
        assertThat(stack.lore()).containsExactly(Component.text("one"), Component.text("two"));
        assertThat(stack.itemModel()).contains(Key.key("fand:test_model"));
        assertThat(stack.customModelData()).contains(new CustomModelData(List.of(1.0F), List.of(true), List.of("demo"), List.of(0x336699)));
        assertThat(stack.enchantmentGlintOverride()).contains(true);
        assertThat(stack.unbreakable()).isTrue();
        assertThat(stack.damage()).contains(3);
        assertThat(stack.maxDamage()).contains(10);
        assertThat(stack.repairCost()).contains(2);
        assertThat(stack.rarity()).contains(ItemRarity.RARE);
        assertThat(stack.customData()).get().extracting(json -> json.get("owner").getAsString()).isEqualTo("test");
    }

    @Test
    void maxStackSizeComponentControlsAmountValidation() {
        var stack = new ItemStack(DIAMOND, 1)
                .withMaxStackSize(99)
                .withAmount(99);

        assertThat(stack.amount()).isEqualTo(99);
        assertThat(stack.maxStackSize()).isEqualTo(99);
        assertThatThrownBy(() -> stack.withAmount(100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds max stack size 99");
    }

    @Test
    void applyComponentsMergesValuesAndRemovals() {
        var base = new ItemStack(DIAMOND, 1)
                .withCustomName(Component.text("before"))
                .withLore(Component.text("lore"));
        var patch = ItemComponents.of(ItemComponentKeys.RARITY, new com.google.gson.JsonPrimitive("epic"))
                .remove(ItemComponentKeys.CUSTOM_NAME);

        var changed = base.applyComponents(patch);

        assertThat(changed.customName()).isEmpty();
        assertThat(changed.lore()).containsExactly(Component.text("lore"));
        assertThat(changed.rarity()).contains(ItemRarity.EPIC);
        assertThat(changed.components().removes(ItemComponentKeys.CUSTOM_NAME)).isTrue();
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
