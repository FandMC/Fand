package io.fand.server.compat.modprotocol.servux;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class ServuxPacketCodecTest {

    @Test
    void hudAndStructureSplitPayloadContainsOnlyNbt() {
        var tag = new CompoundTag();
        tag.putString("Task", "RecipeManager");

        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(ServuxPacketCodec.writeSplitPayload(tag)));
        var decoded = buffer.readNbt();

        assertThat(decoded).isNotNull();
        assertThat(decoded.getStringOr("Task", "")).isEqualTo("RecipeManager");
        assertThat(buffer.isReadable()).isFalse();
    }

    @Test
    void litematicaSplitPayloadKeepsTransactionIdBeforeNbt() {
        var tag = new CompoundTag();
        tag.putString("Task", "BulkEntityReply");

        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(ServuxPacketCodec.writeTransactionalSplitPayload(42, tag)));

        assertThat(buffer.readVarInt()).isEqualTo(42);
        assertThat(buffer.readNbt().getStringOr("Task", "")).isEqualTo("BulkEntityReply");
        assertThat(buffer.isReadable()).isFalse();
    }

    @Test
    void transactionalRequestsExposeTheSkippedTransactionId() {
        var raw = ServuxPacketCodec.buffer();
        raw.writeVarInt(3);
        raw.writeVarInt(77);
        raw.writeBlockPos(new BlockPos(1, 2, 3));

        var packet = ServuxPacketCodec.read(ServuxPacketCodec.bytes(raw));
        var request = packet.transactionalBlockPos();

        assertThat(packet.type()).isEqualTo(3);
        assertThat(request.transactionId()).isEqualTo(77);
        assertThat(request.pos()).isEqualTo(new BlockPos(1, 2, 3));
    }

    @Test
    void c2sSplitReassemblesTransactionalNbtBodyWithoutPacketType() {
        var tag = new CompoundTag();
        tag.putString("Task", "LitematicaPaste");
        var body = ServuxPacketCodec.writeTransactionalSplitPayload(99, tag);
        var first = ServuxPacketCodec.buffer();
        first.writeVarInt(body.length);
        first.writeBytes(body, 0, 2);

        var splitter = new ServuxSplitter();
        assertThat(splitter.receive(UUID.fromString("00000000-0000-0000-0000-000000000001"), ServuxPacketCodec.bytes(first))).isNull();

        var second = ServuxPacketCodec.buffer();
        second.writeBytes(body, 2, body.length - 2);
        var complete = splitter.receive(UUID.fromString("00000000-0000-0000-0000-000000000001"), ServuxPacketCodec.bytes(second));
        var decoded = new FriendlyByteBuf(Unpooled.wrappedBuffer(complete));

        assertThat(decoded.readVarInt()).isEqualTo(99);
        assertThat(decoded.readNbt().getStringOr("Task", "")).isEqualTo("LitematicaPaste");
        assertThat(decoded.isReadable()).isFalse();
    }
}
