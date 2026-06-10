package io.fand.server.world;

import net.minecraft.world.item.ItemStack;

/**
 * Hash key for ItemStack identity used in explosion drop merging.
 *
 * <p>Wraps the stack's item and components; two keys are equal if their stacks
 * can merge per {@link net.minecraft.world.entity.item.ItemEntity#areMergable}.
 * Used to replace O(n) linear search with O(1) hash lookup when collecting
 * explosion drops.
 */
public record ItemStackKey(net.minecraft.world.item.Item item, net.minecraft.core.component.DataComponentMap components) {

    public ItemStackKey(ItemStack stack) {
        this(stack.getItem(), stack.getComponents());
    }
}
