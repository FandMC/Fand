package io.fand.server.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.block.custom.CustomBlockMining;
import io.fand.api.block.custom.CustomBlockType;
import io.fand.api.item.custom.CustomItemType;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.item.component.ItemComponentKeys;
import io.fand.api.item.component.ItemComponents;
import io.fand.api.item.component.ItemKeySet;
import io.fand.api.item.component.ItemTool;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class FandCustomItemRegistryTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);

    @Test
    void registersCreatesAndIdentifiesCustomItems() {
        var registry = new FandCustomItemRegistry();
        var id = Key.key("test:hammer");
        registry.register(CustomItemType.of(id, new ItemStack(DIAMOND, 1)));

        var stack = registry.create(id, 3);

        assertThat(stack.amount()).isEqualTo(3);
        assertThat(stack.type()).isInstanceOf(CustomItemType.class);
        assertThat(stack.type().key()).isEqualTo(id);
        assertThat(registry.customId(stack)).contains(id);
        assertThat(registry.customItem(stack)).get().extracting(CustomItemType::id).isEqualTo(id);
    }

    @Test
    void encodesAndDecodesLogicalTypeWithDefaultComponents() {
        var registry = new FandCustomItemRegistry();
        var id = Key.key("test:hammer");
        var defaults = ItemComponents.EMPTY.withKey(ItemComponentKeys.ITEM_MODEL, Key.key("test:hammer"));
        var registration = registry.register(new CustomItemType(id, DIAMOND, defaults));
        var logical = registration.type().stack(2)
                .withCustomName(Component.text("custom name"));

        var encoded = registry.encode(logical);
        var decoded = registry.decode(encoded);

        assertThat(logical.componentPatch().empty()).isFalse();
        assertThat(logical.itemModel()).contains(Key.key("test:hammer"));
        assertThat(logical.customName()).contains(Component.text("custom name"));
        assertThat(encoded.type()).isEqualTo(DIAMOND);
        assertThat(encoded.itemModel()).contains(Key.key("test:hammer"));
        assertThat(registry.customId(encoded)).contains(id);
        assertThat(decoded.type()).isSameAs(registration.type());
        assertThat(decoded.itemModel()).contains(Key.key("test:hammer"));
        assertThat(decoded.customData()).isEmpty();
    }

    @Test
    void unregisterRemovesTypeButLeavesStackTagReadable() {
        var registry = new FandCustomItemRegistry();
        var id = Key.key("test:hammer");
        var registration = registry.register(CustomItemType.of(id, new ItemStack(DIAMOND, 1)));
        var stack = registry.create(id, 1);

        registration.unregister();

        assertThat(registry.customId(stack)).contains(id);
        assertThat(registry.customItem(stack)).isEmpty();
        assertThatThrownBy(() -> registry.create(id, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customToolRulesMatchBlockIdsAndTags() {
        var customTag = Key.key("test:mineable/hammer");
        var directBlock = CustomBlockType.builder(Key.key("test:direct_ore"), new TestBlockType(Key.key("minecraft:stone")))
                .mining(new CustomBlockMining(4.0F, 6.0F, new TestBlockType(Key.key("minecraft:iron_block")), true))
                .build();
        var taggedBlock = CustomBlockType.builder(Key.key("test:tagged_ore"), new TestBlockType(Key.key("minecraft:stone")))
                .tag(customTag)
                .build();
        var item = CustomItemType.builder(Key.key("test:hammer"), DIAMOND)
                .tool(new ItemTool(List.of(), 1.0F, 1, true))
                .customBlockToolRule(ItemTool.Rule.minesAndDrops(ItemKeySet.of(directBlock.id()), 12.0F))
                .customBlockToolRule(ItemTool.Rule.overrideSpeed(ItemKeySet.tag(customTag), 8.0F))
                .build();

        assertThat(item.customBlockToolRule(directBlock)).get().satisfies(rule -> {
            assertThat(rule.speed()).contains(12.0F);
            assertThat(rule.correctForDrops()).contains(true);
        });
        assertThat(item.customBlockToolRule(taggedBlock)).get().satisfies(rule -> {
            assertThat(rule.speed()).contains(8.0F);
            assertThat(rule.correctForDrops()).isEmpty();
        });
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private record TestBlockType(Key key) implements io.fand.api.block.BlockType {
    }
}
