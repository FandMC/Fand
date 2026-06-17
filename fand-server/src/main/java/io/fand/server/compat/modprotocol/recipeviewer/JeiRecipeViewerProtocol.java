package io.fand.server.compat.modprotocol.recipeviewer;

import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.messaging.PluginMessageRegistration;
import io.fand.server.entity.FandPlayer;
import io.fand.server.hooks.FandHooks;
import io.fand.server.messaging.FandPluginMessaging;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JeiRecipeViewerProtocol implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(JeiRecipeViewerProtocol.class);
    private static final Key REQUEST_CHEAT_PERMISSION = Key.key("jei:request_cheat_permission");
    private static final Key GIVE_ITEM_STACK = Key.key("jei:give_item_stack");
    private static final Key DELETE_PLAYER_ITEM = Key.key("jei:delete_player_item");
    private static final Key SET_HOTBAR_ITEM_STACK = Key.key("jei:set_hotbar_item_stack");
    private static final Key RECIPE_TRANSFER = Key.key("jei:recipe_transfer");
    private static final Key CHEAT_PERMISSION = Key.key("jei:cheat_permission");
    private static final List<String> CHEAT_METHODS = List.of(
            "jei.chat.error.no.cheat.permission.op",
            "jei.chat.error.no.cheat.permission.creative",
            "jei.chat.error.no.cheat.permission.give");

    private final FandPluginMessaging messaging;
    private final List<PluginMessageRegistration> registrations;

    public JeiRecipeViewerProtocol(FandPluginMessaging messaging) {
        this.messaging = messaging;
        this.registrations = List.of(
                messaging.register(REQUEST_CHEAT_PERMISSION, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        sendCheatPermission(fandPlayer.handle());
                    }
                }),
                messaging.register(GIVE_ITEM_STACK, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        handleGive(fandPlayer.handle(), payload);
                    }
                }),
                messaging.register(DELETE_PLAYER_ITEM, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        handleDelete(fandPlayer.handle(), payload);
                    }
                }),
                messaging.register(SET_HOTBAR_ITEM_STACK, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        handleHotbar(fandPlayer.handle(), payload);
                    }
                }),
                messaging.register(RECIPE_TRANSFER, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        handleRecipeTransfer(fandPlayer.handle(), payload);
                    }
                }),
                messaging.register(CHEAT_PERMISSION, PluginMessageDirection.CLIENTBOUND)
        );
    }

    private void sendCheatPermission(ServerPlayer player) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buffer.writeBoolean(hasCheatPermission(player));
        buffer.writeCollection(CHEAT_METHODS, FriendlyByteBuf::writeUtf);
        send(player, CHEAT_PERMISSION, readBytes(buffer));
    }

    private void handleGive(ServerPlayer player, byte[] payload) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), player.registryAccess());
        var stack = ItemStack.STREAM_CODEC.decode(buffer);
        var mode = buffer.readEnum(GiveMode.class);
        if (!hasCheatPermission(player)) {
            sendCheatPermission(player);
            return;
        }
        if (stack.isEmpty()) {
            return;
        }
        if (mode == GiveMode.INVENTORY) {
            giveToInventory(player, stack.copy());
        } else {
            pickupToCursor(player, stack.copy());
        }
    }

    private void handleDelete(ServerPlayer player, byte[] payload) {
        if (!hasCheatPermission(player)) {
            sendCheatPermission(player);
            return;
        }
        var expected = ItemStack.STREAM_CODEC.decode(new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), player.registryAccess()));
        var carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && carried.getItem() == expected.getItem()) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
        }
    }

    private void handleHotbar(ServerPlayer player, byte[] payload) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), player.registryAccess());
        var stack = ItemStack.STREAM_CODEC.decode(buffer);
        int hotbarSlot = buffer.readVarInt();
        if (!hasCheatPermission(player)) {
            sendCheatPermission(player);
            return;
        }
        if (stack.isEmpty() || !Inventory.isHotbarSlot(hotbarSlot)) {
            return;
        }
        player.getInventory().setItem(hotbarSlot, stack.copy());
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private void handleRecipeTransfer(ServerPlayer player, byte[] payload) {
        try {
            RecipeTransfer.transfer(player, new FriendlyByteBuf(Unpooled.wrappedBuffer(payload)));
        } catch (RuntimeException failure) {
            LOGGER.debug("JEI recipe transfer failed for {}", player.getGameProfile().name(), failure);
        }
    }

    private static boolean hasCheatPermission(ServerPlayer player) {
        return player.isCreative() || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static void giveToInventory(ServerPlayer player, ItemStack stack) {
        var copy = stack.copy();
        boolean added = player.getInventory().add(stack);
        if (added && stack.isEmpty()) {
            copy.setCount(1);
            var dropped = player.drop(copy, false);
            if (dropped != null) {
                dropped.makeFakeItem();
            }
        } else if (!stack.isEmpty()) {
            var dropped = player.drop(stack, false);
            if (dropped != null) {
                dropped.setNoPickUpDelay();
                dropped.setTarget(player.getUUID());
            }
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 2.0F);
        player.inventoryMenu.broadcastChanges();
    }

    private static void pickupToCursor(ServerPlayer player, ItemStack stack) {
        AbstractContainerMenu menu = player.containerMenu;
        var carried = menu.getCarried();
        if (!carried.isEmpty() && ItemEntity.areMergable(carried.copyWithCount(1), stack.copyWithCount(1))) {
            carried.setCount(Math.min(carried.getMaxStackSize(), carried.getCount() + stack.getCount()));
        } else if (carried.isEmpty()) {
            menu.setCarried(stack);
        }
        menu.broadcastChanges();
    }

    private static byte[] readBytes(FriendlyByteBuf buffer) {
        var bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    private void send(ServerPlayer player, Key channel, byte[] payload) {
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer != null) {
            messaging.send(fandPlayer, channel, payload);
        }
    }

    @Override
    public void close() {
        registrations.forEach(PluginMessageRegistration::close);
    }

    private enum GiveMode {
        INVENTORY,
        MOUSE_PICKUP
    }
}
