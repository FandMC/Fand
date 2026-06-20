package io.fand.server.compat.modprotocol.recipeviewer;

import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.messaging.PluginMessageRegistration;
import io.fand.server.entity.FandPlayer;
import io.fand.server.hooks.FandHooks;
import io.fand.server.messaging.FandPluginMessaging;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReiRecipeViewerProtocol implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReiRecipeViewerProtocol.class);
    private static final Key DELETE_ITEM = Key.key("roughlyenoughitems:delete_item");
    private static final Key CREATE_ITEM = Key.key("roughlyenoughitems:create_item");
    private static final Key CREATE_ITEM_HOTBAR = Key.key("roughlyenoughitems:create_item_hotbar");
    private static final Key CREATE_ITEM_GRAB = Key.key("roughlyenoughitems:create_item_grab");
    private static final Key MOVE_ITEMS_NEW = Key.key("roughlyenoughitems:move_items_new");
    private static final Key REQUEST_TAGS_C2S = Key.key("roughlyenoughitems:request_tags_c2s");
    private static final Key CREATE_ITEM_MESSAGE = Key.key("roughlyenoughitems:ci_msg");
    private static final Key NOT_ENOUGH_ITEMS = Key.key("roughlyenoughitems:og_not_enough");
    private static final Key SYNC_DISPLAYS = Key.key("roughlyenoughitems:sync_displays");
    private static final Key REQUEST_TAGS_S2C = Key.key("roughlyenoughitems:request_tags_s2c");

    private final FandPluginMessaging messaging;
    private final List<PluginMessageRegistration> registrations;

    public ReiRecipeViewerProtocol(FandPluginMessaging messaging) {
        this.messaging = messaging;
        this.registrations = List.of(
                messaging.register(DELETE_ITEM, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        handleDelete(fandPlayer.handle());
                    }
                }),
                messaging.register(CREATE_ITEM, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        handleCreate(fandPlayer.handle(), payload, CreateMode.INVENTORY);
                    }
                }),
                messaging.register(CREATE_ITEM_GRAB, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        handleCreate(fandPlayer.handle(), payload, CreateMode.CURSOR);
                    }
                }),
                messaging.register(CREATE_ITEM_HOTBAR, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        handleHotbar(fandPlayer.handle(), payload);
                    }
                }),
                messaging.register(MOVE_ITEMS_NEW, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        handleMoveItems(fandPlayer.handle(), payload);
                    }
                }),
                messaging.register(REQUEST_TAGS_C2S, PluginMessageDirection.SERVERBOUND, (player, channel, payload) -> {
                    if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                        handleTagRequest(fandPlayer.handle(), payload);
                    }
                }),
                messaging.register(CREATE_ITEM_MESSAGE, PluginMessageDirection.CLIENTBOUND),
                messaging.register(NOT_ENOUGH_ITEMS, PluginMessageDirection.CLIENTBOUND),
                messaging.register(SYNC_DISPLAYS, PluginMessageDirection.CLIENTBOUND),
                messaging.register(REQUEST_TAGS_S2C, PluginMessageDirection.CLIENTBOUND)
        );
    }

    private void handleDelete(ServerPlayer player) {
        if (!hasCheatPermission(player)) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (!menu.getCarried().isEmpty()) {
            menu.setCarried(ItemStack.EMPTY);
            menu.broadcastChanges();
        }
    }

    private void handleCreate(ServerPlayer player, byte[] payload, CreateMode mode) {
        if (!hasCheatPermission(player)) {
            return;
        }
        var stack = readJsonStack(player, payload);
        if (stack.isEmpty()) {
            return;
        }
        if (mode == CreateMode.INVENTORY) {
            if (player.getInventory().add(stack.copy())) {
                sendCreateMessage(player, stack);
            }
            player.inventoryMenu.broadcastChanges();
            return;
        }
        var menu = player.containerMenu;
        var carried = menu.getCarried();
        var copy = stack.copy();
        if (!carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, copy)) {
            copy.setCount(Math.min(copy.getMaxStackSize(), copy.getCount() + carried.getCount()));
        } else if (!carried.isEmpty()) {
            return;
        }
        menu.setCarried(copy);
        menu.broadcastChanges();
        sendCreateMessage(player, copy);
    }

    private void handleHotbar(ServerPlayer player, byte[] payload) {
        if (!hasCheatPermission(player)) {
            return;
        }
        var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), player.registryAccess());
        var stack = buffer.readLenientJsonWithCodec(ItemStack.OPTIONAL_CODEC);
        int slot = buffer.readVarInt();
        if (!Inventory.isHotbarSlot(slot) || stack.isEmpty()) {
            return;
        }
        player.getInventory().setItem(slot, stack.copy());
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        sendCreateMessage(player, stack);
    }

    private void handleMoveItems(ServerPlayer player, byte[] payload) {
        try {
            var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));
            buffer.readIdentifier();
            boolean shift = buffer.readBoolean();
            var nbt = buffer.readNbt();
            if (nbt != null && nbt.getInt("Version").orElse(-1) == 1) {
                RecipeTransfer.transferReiLike(player, nbt, shift);
            }
        } catch (Throwable failure) {
            LOGGER.debug("REI move-items failed for {}", player.getGameProfile().name(), failure);
        }
    }

    private void handleTagRequest(ServerPlayer player, byte[] payload) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), player.registryAccess());
        UUID request = new UUID(buffer.readLong(), buffer.readLong());
        buffer.readIdentifier();
        var response = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        response.writeLong(request.getMostSignificantBits());
        response.writeLong(request.getLeastSignificantBits());
        response.writeVarInt(0);
        send(player, REQUEST_TAGS_S2C, readBytes(response));
    }

    private void sendCreateMessage(ServerPlayer player, ItemStack stack) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buffer.writeJsonWithCodec(ItemStack.OPTIONAL_CODEC, stack.copy());
        buffer.writeUtf(player.getScoreboardName(), 32767);
        send(player, CREATE_ITEM_MESSAGE, readBytes(buffer));
    }

    private static ItemStack readJsonStack(ServerPlayer player, byte[] payload) {
        return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), player.registryAccess())
                .readLenientJsonWithCodec(ItemStack.OPTIONAL_CODEC);
    }

    private static boolean hasCheatPermission(ServerPlayer player) {
        return player.isCreative() || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
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

    private enum CreateMode {
        INVENTORY,
        CURSOR
    }
}
