package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.item.custom.CustomItemType;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.item.component.ItemKeySet;
import io.fand.api.item.component.ItemTool;
import io.fand.server.item.FandCustomItemRegistry;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class PluginCustomItemRegistryTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);

    @Test
    void pluginCannotUntagForeignCustomItems() {
        var delegate = new FandCustomItemRegistry();
        var demo = new PluginCustomItemRegistry(delegate, new PluginResourceTracker(), "demo");
        var other = new PluginCustomItemRegistry(delegate, new PluginResourceTracker(), "other");
        demo.register(CustomItemType.of(Key.key("demo:hammer"), new ItemStack(DIAMOND, 1)));
        other.register(CustomItemType.of(Key.key("other:wrench"), new ItemStack(DIAMOND, 1)));
        var demoStack = demo.create(Key.key("demo:hammer"), 1);
        var otherStack = other.create(Key.key("other:wrench"), 1);

        assertThat(demo.customId(demo.untag(demoStack))).isEmpty();
        assertThat(demo.untag(otherStack)).isEqualTo(otherStack);
        assertThat(delegate.customId(demo.untag(otherStack))).contains(Key.key("other:wrench"));
    }

    @Test
    void registrationScopesDefaultModelAndCustomBlockRules() {
        var delegate = new FandCustomItemRegistry();
        var demo = new PluginCustomItemRegistry(delegate, new PluginResourceTracker(), "demo");
        var registration = demo.register(CustomItemType.builder(Key.key("input:hammer"), DIAMOND)
                .customBlockToolRule(ItemTool.Rule.minesAndDrops(
                        ItemKeySet.of(Key.key("input:ore")),
                        9.0F))
                .build());

        assertThat(registration.type().id()).isEqualTo(Key.key("demo:hammer"));
        assertThat(registration.type().template().itemModel()).contains(Key.key("demo:hammer"));
        assertThat(registration.type().customBlockToolRules())
                .singleElement()
                .satisfies(rule -> assertThat(rule.blocks().values()).containsExactly(Key.key("demo:ore")));
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
