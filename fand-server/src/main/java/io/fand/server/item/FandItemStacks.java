package io.fand.server.item;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class FandItemStacks {

    private FandItemStacks() {}

    public static net.minecraft.world.item.ItemStack toVanilla(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        ItemType type = stack.type();
        var item = ((FandItemType) requireFandType(type)).handle();
        return new net.minecraft.world.item.ItemStack(item, stack.amount());
    }

    public static ItemStack fromVanilla(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(FandItemType.of(stack.getItem()), stack.getCount());
    }

    public static FandItemType resolve(net.kyori.adventure.key.Key key) {
        var id = Identifier.fromNamespaceAndPath(key.namespace(), key.value());
        return BuiltInRegistries.ITEM.getOptional(id)
                .map(FandItemType::of)
                .orElse(null);
    }

    private static ItemType requireFandType(ItemType type) {
        if (type instanceof FandItemType) {
            return type;
        }
        throw new IllegalArgumentException("ItemType must be obtained from ItemTypes / Server.itemType");
    }
}
