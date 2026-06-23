package io.fand.server.compat.modprotocol.servux;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

final class ServuxPacketCodec {

    private ServuxPacketCodec() {
    }

    static byte[] metadata(int type, CompoundTag tag) {
        var buffer = buffer();
        buffer.writeVarInt(type);
        buffer.writeNbt(tag);
        return bytes(buffer);
    }

    static byte[] blockEntitySimple(BlockPos pos, CompoundTag tag) {
        var buffer = buffer();
        buffer.writeVarInt(ServuxPacketType.S2C_BLOCK_NBT_RESPONSE_SIMPLE.id());
        buffer.writeBlockPos(pos);
        buffer.writeNbt(tag);
        return bytes(buffer);
    }

    static byte[] entitySimple(int entityId, CompoundTag tag) {
        var buffer = buffer();
        buffer.writeVarInt(ServuxPacketType.S2C_ENTITY_NBT_RESPONSE_SIMPLE.id());
        buffer.writeVarInt(entityId);
        buffer.writeNbt(tag);
        return bytes(buffer);
    }

    static byte[] dataSlice(int type, byte[] payload) {
        var buffer = buffer();
        buffer.writeVarInt(type);
        buffer.writeBytes(payload);
        return bytes(buffer);
    }

    static @Nullable Incoming read(byte[] payload) {
        try {
            var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));
            int type = buffer.readVarInt();
            return new Incoming(type, buffer);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static @Nullable CompoundTag readNbt(FriendlyByteBuf buffer) {
        var tag = buffer.readNbt(NbtAccounter.unlimitedHeap());
        return tag instanceof CompoundTag compound ? compound : null;
    }

    static byte[] writeSplitPayload(CompoundTag tag) {
        var body = buffer();
        body.writeNbt(tag);
        return bytes(body);
    }

    static byte[] writeTransactionalSplitPayload(int transactionId, CompoundTag tag) {
        var body = buffer();
        body.writeVarInt(transactionId);
        body.writeNbt(tag);
        return bytes(body);
    }

    static byte[] bytes(FriendlyByteBuf buffer) {
        var payload = new byte[buffer.readableBytes()];
        buffer.readBytes(payload);
        return payload;
    }

    static FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    static byte[] remaining(ByteBuf buffer) {
        var payload = new byte[buffer.readableBytes()];
        buffer.readBytes(payload);
        return payload;
    }

    record Incoming(int type, FriendlyByteBuf buffer) {

        TransactionalBlockPos transactionalBlockPos() {
            int transactionId = -1;
            if (buffer.isReadable()) {
                transactionId = buffer.readVarInt();
            }
            return new TransactionalBlockPos(transactionId, buffer.readBlockPos());
        }

        TransactionalEntityId transactionalEntityId() {
            int transactionId = -1;
            if (buffer.isReadable()) {
                transactionId = buffer.readVarInt();
            }
            return new TransactionalEntityId(transactionId, buffer.readVarInt());
        }

        ChunkPos chunkPos() {
            return buffer.readChunkPos();
        }
    }

    record TransactionalBlockPos(int transactionId, BlockPos pos) {
    }

    record TransactionalEntityId(int transactionId, int entityId) {
    }
}
