package io.fand.testplugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class TestPluginTest {

    @Test
    void normalisesMinecraftKeys() {
        assertThat(TestPlugin.keyString("stone")).isEqualTo("minecraft:stone");
        assertThat(TestPlugin.keyString(" MINECRAFT:DIAMOND ")).isEqualTo("minecraft:diamond");
        assertThat(TestPlugin.keyString("custom:block")).isEqualTo("custom:block");
    }

    @Test
    void matchesValuesByCaseInsensitivePrefix() {
        assertThat(TestPlugin.matching(List.of("minecraft:stone", "minecraft:torch", "minecraft:diamond"), "MINECRAFT:T"))
                .containsExactly("minecraft:torch");
        assertThat(TestPlugin.matching(List.of("alpha", "beta"), ""))
                .containsExactly("alpha", "beta");
    }

    @Test
    void recognisesMuteNextCommand() {
        assertThat(TestPlugin.isMuteNextCommand("!mute-next")).isTrue();
        assertThat(TestPlugin.isMuteNextCommand("  !MUTE-NEXT  ")).isTrue();
        assertThat(TestPlugin.isMuteNextCommand("!where")).isFalse();
    }

    @Test
    void locksBarrierInDemoGui() {
        var barrier = stack("minecraft:barrier");

        assertThat(TestPlugin.isLockedDemoGuiClick(true, InventoryType.CHEST, TestPlugin.DEMO_GUI_LOCKED_SLOT, barrier))
                .isTrue();
    }

    @Test
    void ignoresBarrierOutsideDemoLockedSlot() {
        var barrier = stack("minecraft:barrier");

        assertThat(TestPlugin.isLockedDemoGuiClick(false, InventoryType.CHEST, TestPlugin.DEMO_GUI_LOCKED_SLOT, barrier))
                .isFalse();
        assertThat(TestPlugin.isLockedDemoGuiClick(true, InventoryType.CHEST, TestPlugin.DEMO_GUI_LOCKED_SLOT + 1, barrier))
                .isFalse();
        assertThat(TestPlugin.isLockedDemoGuiClick(true, InventoryType.PLAYER, TestPlugin.DEMO_GUI_LOCKED_SLOT, barrier))
                .isFalse();
        assertThat(TestPlugin.isLockedDemoGuiClick(true, InventoryType.CHEST, TestPlugin.DEMO_GUI_LOCKED_SLOT, ItemStack.EMPTY))
                .isFalse();
    }

    @Test
    void recognisesStackTypeWithImplicitNamespace() {
        assertThat(TestPlugin.isStackType(stack("minecraft:barrier"), "barrier")).isTrue();
        assertThat(TestPlugin.isStackType(stack("minecraft:stone"), "barrier")).isFalse();
        assertThat(TestPlugin.isStackType(ItemStack.EMPTY, "barrier")).isFalse();
    }

    @Test
    void clampsBossBarProgress() {
        assertThat(TestPlugin.boundedBossBarProgress(-0.5F)).isZero();
        assertThat(TestPlugin.boundedBossBarProgress(0.6F)).isEqualTo(0.6F);
        assertThat(TestPlugin.boundedBossBarProgress(1.5F)).isEqualTo(1.0F);
    }

    @Test
    void recognisesFiniteFloatText() {
        assertThat(TestPlugin.isFloat("0.5")).isTrue();
        assertThat(TestPlugin.isFloat("NaN")).isFalse();
        assertThat(TestPlugin.isFloat("hello")).isFalse();
    }

    @Test
    void joinsMessageTextWithFallback() {
        assertThat(TestPlugin.messageText(List.of("hello", "world"), "fallback")).isEqualTo("hello world");
        assertThat(TestPlugin.messageText(List.of(), "fallback")).isEqualTo("fallback");
        assertThat(TestPlugin.messageText(List.of("  "), "fallback")).isEqualTo("fallback");
    }

    @Test
    void splitsDemoTitleAndSubtitle() {
        var explicit = TestPlugin.demoTitle("Main | Sub", "Default", "Default Sub");
        var fallbackSubtitle = TestPlugin.demoTitle("Main", "Default", "Default Sub");
        var fallbackTitle = TestPlugin.demoTitle(" | Sub", "Default", "Default Sub");

        assertThat(explicit.title()).isEqualTo("Main");
        assertThat(explicit.subtitle()).isEqualTo("Sub");
        assertThat(fallbackSubtitle.title()).isEqualTo("Main");
        assertThat(fallbackSubtitle.subtitle()).isEqualTo("Default Sub");
        assertThat(fallbackTitle.title()).isEqualTo("Default");
        assertThat(fallbackTitle.subtitle()).isEqualTo("Sub");
    }

    private static ItemStack stack(String key) {
        return new ItemStack(new TestItemType(Key.key(key), 64), 1);
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
