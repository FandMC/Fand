package io.fand.server.compat.modprotocol.servux;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;

final class ServuxEntityLikeProvider {

    private final ServuxProtocol protocol;
    private final ServuxProtocol.ConfigView config;
    private final net.kyori.adventure.key.Key channel;
    private final String providerName;
    private final int protocolVersion;
    private final boolean stripBlockEntityMetadata;

    ServuxEntityLikeProvider(
            ServuxProtocol protocol,
            ServuxProtocol.ConfigView config,
            net.kyori.adventure.key.Key channel,
            String providerName,
            int protocolVersion,
            boolean stripBlockEntityMetadata
    ) {
        this.protocol = protocol;
        this.config = config;
        this.channel = channel;
        this.providerName = providerName;
        this.protocolVersion = protocolVersion;
        this.stripBlockEntityMetadata = stripBlockEntityMetadata;
    }

    void handle(ServerPlayer player, ServuxPacketCodec.Incoming packet) {
        switch (packet.type()) {
            case 2 -> sendMetadata(player);
            case 3 -> sendBlockEntity(player, packet.transactionalBlockPos().pos());
            case 4 -> sendEntity(player, packet.transactionalEntityId().entityId());
            default -> {
            }
        }
    }

    void sendMetadata(ServerPlayer player) {
        if (!enabled() || !ServuxPermissions.has(player, permissionLevel())) {
            return;
        }
        protocol.send(player, channel, ServuxPacketCodec.metadata(ServuxPacketType.S2C_METADATA.id(), metadata()));
    }

    private void sendBlockEntity(ServerPlayer player, BlockPos pos) {
        if (!enabled() || !ServuxPermissions.has(player, permissionLevel())) {
            return;
        }
        var blockEntity = player.level().getBlockEntity(pos);
        var tag = blockEntity == null
                ? new CompoundTag()
                : stripBlockEntityMetadata ? ServuxNbt.blockEntityWithoutMetadata(blockEntity) : ServuxNbt.blockEntityFull(blockEntity);
        protocol.send(player, channel, ServuxPacketCodec.blockEntitySimple(pos, tag));
    }

    private void sendEntity(ServerPlayer player, int entityId) {
        if (!enabled() || !ServuxPermissions.has(player, permissionLevel())) {
            return;
        }
        var entity = player.level().getEntity(entityId);
        if (entity == null) {
            return;
        }
        var tag = ServuxNbt.entity(entity);
        if (entity.getType() == EntityTypes.PLAYER) {
            if (!config.allowPlayerInventory() || !ServuxPermissions.has(player, config.playerInventoryPermissionLevel())) {
                tag.remove("Inventory");
                tag.put("Inventory", new net.minecraft.nbt.ListTag());
            }
            if (!config.allowPlayerEnderItems() || !ServuxPermissions.has(player, config.playerEnderItemsPermissionLevel())) {
                tag.remove("EnderItems");
                tag.put("EnderItems", new net.minecraft.nbt.ListTag());
            }
        }
        protocol.send(player, channel, ServuxPacketCodec.entitySimple(entityId, tag));
    }

    private CompoundTag metadata() {
        var tag = new CompoundTag();
        tag.putString("name", providerName);
        tag.putString("id", channel.asString());
        tag.putInt("version", protocolVersion);
        tag.putString("servux", ServuxProtocol.VERSION_STRING);
        if (channel.equals(ServuxChannels.TWEAKS) && config.stackableShulkers()) {
            tag.putBoolean("stackingShulkers", true);
            tag.putInt("stackingShulkersMax", config.stackableShulkerSize());
        }
        return tag;
    }

    private boolean enabled() {
        if (channel.equals(ServuxChannels.ENTITIES)) {
            return config.entityDataEnabled();
        }
        return config.tweaksEnabled();
    }

    private int permissionLevel() {
        if (channel.equals(ServuxChannels.ENTITIES)) {
            return config.entityPermissionLevel();
        }
        return config.tweaksPermissionLevel();
    }
}
