package io.fand.server.compat.modprotocol.recipeviewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class RecipeTransfer {

    private RecipeTransfer() {
    }

    static void transfer(ServerPlayer player, FriendlyByteBuf buffer) {
        int operationCount = buffer.readVarInt();
        var operations = new ArrayList<Operation>(operationCount);
        for (int i = 0; i < operationCount; i++) {
            operations.add(new Operation(buffer.readVarInt(), buffer.readVarInt()));
        }
        var craftingSlots = readSlots(player, buffer);
        var inventorySlots = readSlots(player, buffer);
        boolean maxTransfer = buffer.readBoolean();
        boolean requireCompleteSets = buffer.readBoolean();
        transfer(player, operations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets);
    }

    static void transferReiLike(ServerPlayer player, CompoundTag tag, boolean maxTransfer) {
        var inputSlots = tag.getListOrEmpty("InputSlots");
        var inventorySlots = tag.getListOrEmpty("InventorySlots");
        if (inputSlots.isEmpty() || inventorySlots.isEmpty()) {
            return;
        }
        var crafting = new ArrayList<Slot>();
        for (var entry : inputSlots) {
            if (entry instanceof CompoundTag slotTag) {
                slotTag.getInt("Index").ifPresent(index -> addSlot(player, crafting, index));
            }
        }
        var inventory = new ArrayList<Slot>();
        for (var entry : inventorySlots) {
            if (entry instanceof CompoundTag slotTag) {
                slotTag.getInt("Index").ifPresent(index -> addSlot(player, inventory, index));
            }
        }
        if (crafting.isEmpty() || inventory.isEmpty()) {
            return;
        }
        var operations = new ArrayList<Operation>();
        for (int i = 0; i < Math.min(crafting.size(), inventory.size()); i++) {
            operations.add(new Operation(inventory.get(i).index, crafting.get(i).index));
        }
        transfer(player, operations, crafting, inventory, maxTransfer, false);
    }

    private static void transfer(
            ServerPlayer player,
            List<Operation> operations,
            List<Slot> craftingSlots,
            List<Slot> inventorySlots,
            boolean maxTransfer,
            boolean requireCompleteSets
    ) {
        var menu = player.containerMenu;
        var required = new HashMap<Slot, ItemStack>();
        for (var operation : operations) {
            var inventorySlot = menu.getSlot(operation.inventorySlot);
            var craftingSlot = menu.getSlot(operation.craftingSlot);
            if (!inventorySlot.allowModification(player) || inventorySlot.getItem().isEmpty()) {
                return;
            }
            var single = inventorySlot.getItem().copyWithCount(1);
            required.put(craftingSlot, single);
        }
        if (required.isEmpty()) {
            return;
        }
        var taken = new HashMap<Slot, ItemStack>();
        do {
            var oneSet = takeOneSet(player, required, craftingSlots, inventorySlots, requireCompleteSets || !maxTransfer);
            if (oneSet.isEmpty()) {
                break;
            }
            oneSet.forEach((slot, stack) -> taken.merge(slot, stack, (left, right) -> {
                left.grow(right.getCount());
                return left;
            }));
        } while (maxTransfer);
        if (taken.isEmpty()) {
            return;
        }
        var cleared = new ArrayList<ItemStack>();
        for (var slot : craftingSlots) {
            if (slot.mayPickup(player) && !slot.getItem().isEmpty() && slot.mayPlace(slot.getItem())) {
                cleared.add(slot.safeTake(Integer.MAX_VALUE, Integer.MAX_VALUE, player));
            }
        }
        taken.forEach((slot, stack) -> {
            var remainder = slot.safeInsert(stack);
            if (!remainder.isEmpty()) {
                cleared.add(remainder);
            }
        });
        stow(player, inventorySlots, cleared);
        menu.broadcastChanges();
    }

    private static HashMap<Slot, ItemStack> takeOneSet(
            ServerPlayer player,
            HashMap<Slot, ItemStack> required,
            List<Slot> craftingSlots,
            List<Slot> inventorySlots,
            boolean requireComplete
    ) {
        var original = new HashMap<Slot, ItemStack>();
        var result = new HashMap<Slot, ItemStack>();
        for (var entry : required.entrySet()) {
            var source = findSource(player, entry.getValue(), craftingSlots, inventorySlots);
            if (source == null) {
                if (requireComplete) {
                    original.forEach((slot, stack) -> slot.set(stack));
                    return new HashMap<>();
                }
                continue;
            }
            original.putIfAbsent(source, source.getItem().copy());
            result.put(entry.getKey(), source.safeTake(1, Integer.MAX_VALUE, player));
        }
        return result;
    }

    private static Slot findSource(ServerPlayer player, ItemStack required, List<Slot> craftingSlots, List<Slot> inventorySlots) {
        for (var slot : craftingSlots) {
            if (matches(player, slot, required)) {
                return slot;
            }
        }
        for (var slot : inventorySlots) {
            if (matches(player, slot, required)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean matches(ServerPlayer player, Slot slot, ItemStack required) {
        return slot.allowModification(player) && ItemStack.isSameItemSameComponents(slot.getItem(), required);
    }

    private static void stow(ServerPlayer player, List<Slot> slots, List<ItemStack> stacks) {
        for (var stack : stacks) {
            var remainder = stack.copy();
            for (var slot : slots) {
                if (remainder.isEmpty()) {
                    break;
                }
                if (slot.mayPickup(player)) {
                    remainder = slot.safeInsert(remainder);
                }
            }
            if (!remainder.isEmpty() && !player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
        }
    }

    private static List<Slot> readSlots(ServerPlayer player, FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        var slots = new ArrayList<Slot>(count);
        for (int i = 0; i < count; i++) {
            addSlot(player, slots, buffer.readVarInt());
        }
        return slots;
    }

    private static void addSlot(ServerPlayer player, List<Slot> slots, int index) {
        if (index >= 0 && index < player.containerMenu.slots.size()) {
            slots.add(player.containerMenu.getSlot(index));
        }
    }

    private record Operation(int inventorySlot, int craftingSlot) {
    }
}
