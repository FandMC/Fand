package io.fand.server.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.ReusablePacketEncoding;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class AsyncChunkPacketSenderTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void chunkPacketsSupportReusableEncoding() {
        assertThat(ReusablePacketEncoding.class).isAssignableFrom(ClientboundLevelChunkWithLightPacket.class);
    }

    @Test
    void chunkPacketCanEncodeFromSnapshotWithoutLiveChunk() throws Exception {
        ChunkPos chunkPos = new ChunkPos(4, -7);
        var packet = new ClientboundLevelChunkWithLightPacket(
                chunkPos,
                snapshot(new byte[0]),
                new ClientboundLightUpdatePacketData(new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(emptyLightUpdate()), RegistryAccess.EMPTY), chunkPos.x(), chunkPos.z()));

        var output = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ClientboundLevelChunkWithLightPacket.STREAM_CODEC.encode(output, packet);
            assertThat(output.readableBytes()).isGreaterThan(0);
        } finally {
            output.release();
        }
    }

    @Test
    void chunkPacketStoresPreparedNetworkFrame() throws Exception {
        ChunkPos chunkPos = new ChunkPos(4, -7);
        var packet = new ClientboundLevelChunkWithLightPacket(
                chunkPos,
                snapshot(new byte[0]),
                new ClientboundLightUpdatePacketData(new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(emptyLightUpdate()), RegistryAccess.EMPTY), chunkPos.x(), chunkPos.z()));

        packet.fand$cachePreparedFrame(new byte[] {1, 2, 3});

        assertThat(packet.fand$preparedFrame()).containsExactly(1, 2, 3);
    }

    private static ClientboundLevelChunkPacketData.FandSnapshot snapshot(byte[] chunkData) throws Exception {
        Constructor<ClientboundLevelChunkPacketData.FandSnapshot> constructor =
                ClientboundLevelChunkPacketData.FandSnapshot.class.getDeclaredConstructor(Map.class, byte[].class, List.class);
        constructor.setAccessible(true);
        return constructor.newInstance(Map.of(), chunkData, List.of());
    }

    private static byte[] emptyLightUpdate() {
        var output = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            output.writeBitSet(new java.util.BitSet());
            output.writeBitSet(new java.util.BitSet());
            output.writeBitSet(new java.util.BitSet());
            output.writeBitSet(new java.util.BitSet());
            output.writeVarInt(0);
            output.writeVarInt(0);
            byte[] data = new byte[output.readableBytes()];
            output.getBytes(output.readerIndex(), data);
            return data;
        } finally {
            output.release();
        }
    }

}
