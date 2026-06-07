package io.fand.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Player;
import io.fand.api.event.entity.EntityDamageEvent;
import io.fand.api.event.inventory.ClickType;
import io.fand.api.event.inventory.InventoryClickEvent;
import io.fand.api.event.player.PlayerInteractEvent;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import java.lang.reflect.Proxy;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class EventPayloadTest {

    @Test
    void playerInteractEventCarriesHandItem() {
        var player = proxy(Player.class);
        var item = new ItemStack(new TestItemType(Key.key("minecraft:compass"), 64), 1);

        var event = new PlayerInteractEvent(
                player,
                PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
                PlayerInteractEvent.Hand.MAIN_HAND,
                Optional.empty(),
                item);

        assertThat(event.player()).isSameAs(player);
        assertThat(event.item()).isSameAs(item);
        assertThat(event.block()).isEmpty();
    }

    @Test
    void playerInteractEventKeepsLegacyConstructorEmptyItem() {
        var event = new PlayerInteractEvent(
                proxy(Player.class),
                PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
                PlayerInteractEvent.Hand.MAIN_HAND,
                Optional.empty());

        assertThat(event.item()).isEqualTo(ItemStack.EMPTY);
    }

    @Test
    void inventoryClickEventCarriesSlotContext() {
        var inventory = proxy(Inventory.class);
        var current = new ItemStack(new TestItemType(Key.key("minecraft:stone"), 64), 3);
        var cursor = new ItemStack(new TestItemType(Key.key("minecraft:diamond"), 64), 1);

        var event = new InventoryClickEvent(
                proxy(Player.class),
                inventory,
                14,
                14,
                -1,
                5,
                ClickType.SWAP,
                2,
                current,
                cursor);

        assertThat(event.inventory()).isSameAs(inventory);
        assertThat(event.slot()).isEqualTo(14);
        assertThat(event.rawSlot()).isEqualTo(14);
        assertThat(event.containerSlot()).isEqualTo(-1);
        assertThat(event.playerInventorySlot()).isEqualTo(5);
        assertThat(event.playerInventoryClick()).isTrue();
        assertThat(event.containerClick()).isFalse();
        assertThat(event.outsideClick()).isFalse();
        assertThat(event.hotbarButton()).isEqualTo(2);
    }

    @Test
    void inventoryClickEventKeepsLegacyConstructorDefaults() {
        var event = new InventoryClickEvent(
                proxy(Player.class),
                proxy(Inventory.class),
                InventoryClickEvent.OUTSIDE,
                ClickType.DROP,
                0,
                ItemStack.EMPTY,
                ItemStack.EMPTY);

        assertThat(event.rawSlot()).isEqualTo(InventoryClickEvent.OUTSIDE);
        assertThat(event.containerSlot()).isEqualTo(-1);
        assertThat(event.playerInventorySlot()).isEqualTo(-1);
        assertThat(event.outsideClick()).isTrue();
        assertThat(event.hotbarButton()).isEqualTo(-1);
    }

    @Test
    void entityDamageEventCarriesSourceEntities() {
        var victim = proxy(LivingEntity.class);
        var direct = proxy(LivingEntity.class);
        var attacker = proxy(LivingEntity.class);

        var event = new EntityDamageEvent(victim, "minecraft:player_attack", 4.0, Optional.of(direct), Optional.of(attacker));

        assertThat(event.entity()).isSameAs(victim);
        assertThat(event.directEntity()).contains(direct);
        assertThat(event.attacker()).contains(attacker);
        assertThat(event.amount()).isEqualTo(4.0);
    }

    @Test
    void entityDamageEventKeepsLegacyConstructorEmptySources() {
        var event = new EntityDamageEvent(proxy(LivingEntity.class), "minecraft:fall", 2.0);

        assertThat(event.directEntity()).isEmpty();
        assertThat(event.attacker()).isEmpty();
    }

    private static <T> T proxy(Class<T> type) {
        Object instance = Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + " proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> args != null && args.length == 1 && proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
        return type.cast(instance);
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
