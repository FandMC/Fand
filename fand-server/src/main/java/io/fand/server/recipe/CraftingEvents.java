package io.fand.server.recipe;

import io.fand.api.event.inventory.ClickType;
import io.fand.api.event.inventory.CraftItemEvent;
import io.fand.api.event.inventory.PrepareItemCraftEvent;
import io.fand.server.hooks.FandHooks;
import io.fand.server.inventory.ContainerMenuView;
import io.fand.server.item.FandItemStacks;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CraftingEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(CraftingEvents.class);
    private static final ThreadLocal<ClickType> CLICK_TYPE = new ThreadLocal<>();

    private CraftingEvents() {
    }

    public static void withClickType(ClickType clickType, Runnable task) {
        ClickType previous = CLICK_TYPE.get();
        CLICK_TYPE.set(clickType);
        try {
            task.run();
        } finally {
            if (previous == null) {
                CLICK_TYPE.remove();
            } else {
                CLICK_TYPE.set(previous);
            }
        }
    }

    public static net.minecraft.world.item.ItemStack firePrepare(
            ServerPlayer player,
            AbstractContainerMenu menu,
            @Nullable RecipeHolder<?> recipe,
            net.minecraft.world.item.ItemStack result
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PrepareItemCraftEvent.class)) {
            return result;
        }
        var fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return result;
        }
        var event = new PrepareItemCraftEvent(
                fandPlayer,
                new ContainerMenuView(menu),
                Optional.ofNullable(recipe).map(FandRecipes::fromVanilla),
                FandItemStacks.fromVanilla(result));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PrepareItemCraftEvent listener failed", failure);
            return result;
        }
        try {
            return FandItemStacks.toVanilla(event.result());
        } catch (RuntimeException failure) {
            LOGGER.warn("PrepareItemCraftEvent supplied an invalid result item", failure);
            return result;
        }
    }

    public static boolean fireCraft(
            ServerPlayer player,
            AbstractContainerMenu menu,
            @Nullable RecipeHolder<?> recipe,
            net.minecraft.world.item.ItemStack result,
            ClickType clickType
    ) {
        ClickType currentClickType = CLICK_TYPE.get();
        if (currentClickType == null || isQuickMoveCraft()) {
            return true;
        }
        return fireCraftNow(player, menu, recipe, result, currentClickType);
    }

    public static boolean fireCraftNow(
            ServerPlayer player,
            AbstractContainerMenu menu,
            @Nullable RecipeHolder<?> recipe,
            net.minecraft.world.item.ItemStack result,
            ClickType clickType
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(CraftItemEvent.class)) {
            return true;
        }
        var fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        var event = new CraftItemEvent(
                fandPlayer,
                new ContainerMenuView(menu),
                Optional.ofNullable(recipe).map(FandRecipes::fromVanilla),
                FandItemStacks.fromVanilla(result),
                currentClickType(clickType));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("CraftItemEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    private static ClickType currentClickType(ClickType fallback) {
        ClickType clickType = CLICK_TYPE.get();
        return clickType == null ? fallback : clickType;
    }

    private static boolean isQuickMoveCraft() {
        ClickType clickType = CLICK_TYPE.get();
        return clickType == ClickType.QUICK_MOVE || clickType == ClickType.QUICK_MOVE_ALL;
    }
}
