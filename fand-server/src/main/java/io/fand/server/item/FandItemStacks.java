package io.fand.server.item;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.server.hooks.FandHooks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class FandItemStacks {

    private FandItemStacks() {}

    public static void useRegistries(RegistryAccess access) {
        useRegistries((HolderLookup.Provider) access);
    }

    public static void useRegistries(HolderLookup.Provider access) {
        ItemComponentBridge.useRegistries(access);
    }

    public static net.minecraft.world.item.ItemStack toVanilla(ItemStack stack) {
        if (stack == null || stack.empty()) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        var encoded = FandHooks.customItems().encode(stack);
        ItemType type = encoded.type();
        var item = ((FandItemType) requireFandType(type)).handle();
        var vanilla = new net.minecraft.world.item.ItemStack(item, encoded.amount());
        if (!encoded.components().empty()) {
            vanilla.applyComponents(ItemComponentBridge.toVanilla(encoded.components()));
            net.minecraft.world.item.ItemStack.validateStrict(vanilla)
                    .getOrThrow(error -> new IllegalArgumentException("Invalid item stack components: " + error));
        }
        return vanilla;
    }

    public static ItemStack fromVanilla(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var physical = new ItemStack(
                FandItemType.of(stack.getItem()),
                stack.getCount(),
                ItemComponentBridge.fromVanilla(stack.getComponentsPatch()));
        return FandHooks.customItems().decode(physical);
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
