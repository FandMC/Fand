package io.fand.api.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.block.Block;
import io.fand.api.block.BlockStateSnapshot;
import io.fand.api.block.BlockType;
import io.fand.api.entity.Entity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Player;
import io.fand.api.entity.PlayerInput;
import io.fand.api.event.block.BlockStateChangeEvent;
import io.fand.api.event.entity.EntityKineticWeaponHitEvent;
import io.fand.api.event.entity.EntityMoveItemEvent;
import io.fand.api.event.inventory.CrafterCraftEvent;
import io.fand.api.event.inventory.ShelfItemSwapEvent;
import io.fand.api.event.player.PlayerClientLoadedEvent;
import io.fand.api.event.player.PlayerInputEvent;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.item.component.ItemEquipmentSlot;
import io.fand.api.recipe.Recipe;
import io.fand.api.world.Vector3;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ModernEventPayloadTest {

    @Test
    void blockStateChangeCarriesImmutablePropertySnapshots() {
        var properties = new java.util.LinkedHashMap<>(Map.of("active", "false"));
        var oldState = new BlockStateSnapshot(proxy(BlockType.class), properties);
        var newState = new BlockStateSnapshot(proxy(BlockType.class), Map.of("active", "true"));
        var event = new BlockStateChangeEvent(proxy(Block.class), oldState, newState, 3);
        properties.put("active", "changed-after-construction");

        assertThat(event.oldState().property("active")).contains("false");
        assertThat(event.newState().properties()).containsEntry("active", "true");
        assertThatThrownBy(() -> event.oldState().properties().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void playerInputCanBeReplacedAndClientLoadedMarksInitialJoin() {
        var player = proxy(Player.class);
        var previous = PlayerInput.NONE;
        var replacement = PlayerInput.builder().forward(true).sprint(true).build();
        var input = new PlayerInputEvent(player, previous, PlayerInput.builder().jump(true).build());
        input.setInput(replacement);
        input.setCancelled(true);
        var loaded = new PlayerClientLoadedEvent(player, true);

        assertThat(input.previousInput()).isSameAs(previous);
        assertThat(input.input()).isEqualTo(replacement);
        assertThat(input.cancelled()).isTrue();
        assertThat(loaded.initialJoin()).isTrue();
    }

    @Test
    void shelfAndCrafterEventsCopyOperationPlans() {
        var player = proxy(Player.class);
        var block = proxy(Block.class);
        var exchanges = new ArrayList<ShelfItemSwapEvent.Exchange>();
        exchanges.add(new ShelfItemSwapEvent.Exchange(block, 1, 4, ItemStack.EMPTY, ItemStack.EMPTY));
        var shelf = new ShelfItemSwapEvent(player, block, true, exchanges);
        exchanges.clear();

        var inventory = proxy(Inventory.class, Map.of("size", 9));
        var crafter = new CrafterCraftEvent(
                block,
                inventory,
                proxy(Recipe.class),
                List.of(ItemStack.EMPTY),
                ItemStack.EMPTY,
                List.of(ItemStack.EMPTY));

        assertThat(shelf.exchanges()).hasSize(1);
        assertThat(shelf.powered()).isTrue();
        assertThatThrownBy(() -> crafter.setRemainingItems(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void entityMoveAndKineticHitExposeMutableDecisions() {
        var living = proxy(LivingEntity.class);
        var target = proxy(Entity.class);
        var inventory = proxy(Inventory.class, Map.of("size", 2));
        var move = new EntityMoveItemEvent(
                living,
                inventory,
                1,
                ItemEquipmentSlot.MAINHAND,
                EntityMoveItemEvent.Action.PICK_UP_FROM_CONTAINER,
                ItemStack.EMPTY);
        move.setCancelled(true);

        var kinetic = new EntityKineticWeaponHitEvent(
                living,
                target,
                ItemStack.EMPTY,
                ItemEquipmentSlot.MAINHAND,
                4,
                new Vector3(1, 0, 0),
                Vector3.ZERO,
                20,
                0,
                20,
                7,
                true,
                true,
                true);
        kinetic.setDamage(3.5F);
        kinetic.setDismounts(false);

        assertThat(move.containerSlot()).isEqualTo(1);
        assertThat(move.cancelled()).isTrue();
        assertThat(kinetic.damage()).isEqualTo(3.5F);
        assertThat(kinetic.dismounts()).isFalse();
        assertThatThrownBy(() -> kinetic.setDamage(Float.NaN)).isInstanceOf(IllegalArgumentException.class);
    }

    private static <T> T proxy(Class<T> type) {
        return proxy(type, Map.of());
    }

    private static <T> T proxy(Class<T> type, Map<String, Object> values) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (values.containsKey(method.getName())) return values.get(method.getName());
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            if (method.getReturnType() == long.class) return 0L;
            if (method.getReturnType() == float.class) return 0.0F;
            if (method.getReturnType() == double.class) return 0.0D;
            return null;
        }));
    }
}
