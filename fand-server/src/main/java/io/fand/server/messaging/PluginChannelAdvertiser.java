package io.fand.server.messaging;

import io.fand.api.entity.Player;
import io.fand.server.entity.FandPlayer;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

final class PluginChannelAdvertiser {

    private static final Key LEGACY_REGISTER = Key.key("minecraft:register");
    private static final Key LEGACY_UNREGISTER = Key.key("minecraft:unregister");
    private static final Key COMMON_VERSION_CHANNEL = Key.key("c:version");
    private static final Key COMMON_REGISTER = Key.key("c:register");
    private static final String COMMON_PLAY_PROTOCOL = "play";
    static final int COMMON_PACKET_VERSION = 1;

    private final java.util.function.Supplier<Collection<? extends Player>> players;

    PluginChannelAdvertiser(java.util.function.Supplier<Collection<? extends Player>> players) {
        this.players = Objects.requireNonNull(players, "players");
    }

    void broadcast(Collection<Key> channels) {
        var snapshot = sorted(channels);
        if (snapshot.isEmpty()) {
            return;
        }
        for (var player : players.get()) {
            send(player, snapshot);
        }
    }

    void broadcastUnregister(Collection<Key> channels) {
        var snapshot = sorted(channels);
        if (snapshot.isEmpty()) {
            return;
        }
        for (var player : players.get()) {
            sendUnregister(player, snapshot);
        }
    }

    void send(Player player, Collection<Key> channels) {
        Objects.requireNonNull(player, "player");
        var snapshot = sorted(channels);
        if (snapshot.isEmpty() || !(player instanceof FandPlayer fandPlayer)) {
            return;
        }
        var handle = fandPlayer.handle();
        var server = handle.level().getServer();
        if (server == null) {
            return;
        }
        Runnable send = () -> {
            if (handle.connection == null) {
                return;
            }
            handle.connection.send(packet(LEGACY_REGISTER, legacyPayload(snapshot)));
        };
        run(server, send);
    }

    void sendUnregister(Player player, Collection<Key> channels) {
        Objects.requireNonNull(player, "player");
        var snapshot = sorted(channels);
        if (snapshot.isEmpty() || !(player instanceof FandPlayer fandPlayer)) {
            return;
        }
        var handle = fandPlayer.handle();
        var server = handle.level().getServer();
        if (server == null) {
            return;
        }
        Runnable send = () -> {
            if (handle.connection == null) {
                return;
            }
            handle.connection.send(packet(LEGACY_UNREGISTER, legacyPayload(snapshot)));
        };
        run(server, send);
    }

    static List<Key> sorted(Collection<Key> channels) {
        return channels.stream()
                .distinct()
                .sorted(Comparator.comparing(Key::asString))
                .toList();
    }

    static ClientboundCustomPayloadPacket legacyRegisterPacket(Collection<Key> channels) {
        return packet(LEGACY_REGISTER, legacyPayload(sorted(channels)));
    }

    static ClientboundCustomPayloadPacket commonConfigurationRegisterPacket() {
        return commonConfigurationRegisterPacket(List.of());
    }

    static ClientboundCustomPayloadPacket commonConfigurationRegisterPacket(Collection<Key> configurationChannels) {
        var channels = new java.util.ArrayList<Key>();
        channels.add(COMMON_REGISTER);
        channels.add(COMMON_VERSION_CHANNEL);
        channels.addAll(configurationChannels);
        return legacyRegisterPacket(channels);
    }

    static ClientboundCustomPayloadPacket commonVersionPacket() {
        return packet(COMMON_VERSION_CHANNEL, commonVersionPayload());
    }

    static ClientboundCustomPayloadPacket commonRegisterPacket(Collection<Key> channels, int version) {
        return packet(COMMON_REGISTER, commonRegisterPayload(sorted(channels), version));
    }

    static boolean isLegacyRegister(Identifier id) {
        return identifier(LEGACY_REGISTER).equals(id);
    }

    static boolean isCommonVersion(Identifier id) {
        return identifier(COMMON_VERSION_CHANNEL).equals(id);
    }

    static boolean isCommonRegister(Identifier id) {
        return identifier(COMMON_REGISTER).equals(id);
    }

    private static ClientboundCustomPayloadPacket packet(Key channel, byte[] payload) {
        return new ClientboundCustomPayloadPacket(new DiscardedPayload(identifier(channel), payload));
    }

    static byte[] legacyPayload(List<Key> channels) {
        var joined = String.join("\0", channels.stream().map(Key::asString).toList());
        return joined.getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] commonVersionPayload() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarIntArray(new int[] {COMMON_PACKET_VERSION});
        return readBytes(buffer);
    }

    static byte[] commonRegisterPayload(List<Key> channels) {
        return commonRegisterPayload(channels, COMMON_PACKET_VERSION);
    }

    static byte[] commonRegisterPayload(List<Key> channels, int version) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(version);
        buffer.writeUtf(COMMON_PLAY_PROTOCOL);
        buffer.writeVarInt(channels.size());
        for (var channel : channels) {
            buffer.writeIdentifier(identifier(channel));
        }
        return readBytes(buffer);
    }

    private static byte[] readBytes(FriendlyByteBuf buffer) {
        var payload = new byte[buffer.readableBytes()];
        buffer.readBytes(payload);
        return payload;
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static void run(MinecraftServer server, Runnable task) {
        if (server.isSameThread()) {
            task.run();
        } else {
            server.execute(task);
        }
    }
}
