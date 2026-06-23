package io.fand.server.compat.modprotocol.servux;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.kyori.adventure.key.Key;

final class ServuxSplitter {

    private static final int MAX_TOTAL_PER_PACKET_S2C = 1_048_576;
    private static final int MAX_PAYLOAD_PER_PACKET_S2C = MAX_TOTAL_PER_PACKET_S2C - 5;
    private static final int DEFAULT_MAX_RECEIVE_SIZE_C2S = 67_108_864;

    private final Map<UUID, ReadingSession> sessions = new HashMap<>();

    void send(Key channel, byte[] payload, int dataPacketType, BiConsumer<Key, byte[]> sender) {
        for (int offset = 0; offset < payload.length; offset += MAX_PAYLOAD_PER_PACKET_S2C) {
            int length = Math.min(payload.length - offset, MAX_PAYLOAD_PER_PACKET_S2C);
            var buffer = ServuxPacketCodec.buffer();
            if (offset == 0) {
                buffer.writeVarInt(payload.length);
            }
            buffer.writeBytes(payload, offset, length);
            sender.accept(channel, ServuxPacketCodec.dataSlice(dataPacketType, ServuxPacketCodec.bytes(buffer)));
        }
    }

    byte[] receive(UUID player, byte[] slice) {
        return sessions.computeIfAbsent(player, ignored -> new ReadingSession()).receive(slice);
    }

    void forget(UUID player) {
        sessions.remove(player);
    }

    void clear() {
        sessions.clear();
    }

    private static final class ReadingSession {

        private int expectedSize = -1;
        private byte[] received;
        private int offset;

        private byte[] receive(byte[] slice) {
            var source = ServuxPacketCodec.buffer();
            source.writeBytes(slice);
            if (expectedSize < 0) {
                expectedSize = source.readVarInt();
                if (expectedSize < 0 || expectedSize > DEFAULT_MAX_RECEIVE_SIZE_C2S) {
                    throw new IllegalArgumentException("Servux payload too large: " + expectedSize);
                }
                received = new byte[expectedSize];
            }
            int length = Math.min(source.readableBytes(), expectedSize - offset);
            source.readBytes(received, offset, length);
            offset += length;
            if (offset >= expectedSize) {
                var complete = received;
                expectedSize = -1;
                received = null;
                offset = 0;
                return complete;
            }
            return null;
        }
    }
}
