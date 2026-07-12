package io.fand.server.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.entity.Player;
import io.fand.api.messaging.PluginMessageChannel;
import io.fand.api.player.PlayerProfile;
import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.messaging.PluginMessageHandler;
import io.fand.api.packet.CustomPacket;
import io.fand.api.packet.PacketContext;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketProtocol;
import io.fand.server.network.packet.PacketRegistryImpl;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import org.junit.jupiter.api.Test;

final class FandPluginMessagingTest {

    @Test
    void clientboundRegistrationIsVisibleAndDoesNotNeedPacketHandler() {
        var packets = new PacketRegistryImpl();
        var messaging = new FandPluginMessaging(packets);
        var channel = Key.key("example:client");

        var registration = messaging.register(channel, PluginMessageDirection.CLIENTBOUND);

        assertThat(messaging.channels()).containsExactly(new io.fand.api.messaging.PluginMessageChannel(
                channel,
                PluginMessageDirection.CLIENTBOUND));
        assertThat(packets.customHandler(PacketProtocol.PLAY, channel)).isEmpty();

        registration.close();

        assertThat(messaging.channels()).isEmpty();
    }

    @Test
    void serverboundRegistrationInstallsPacketHandlerAndReleasesIt() {
        var packets = new PacketRegistryImpl();
        var messaging = new FandPluginMessaging(packets);
        var channel = Key.key("example:server");

        var registration = messaging.register(channel, PluginMessageDirection.SERVERBOUND, noopHandler());

        assertThat(packets.customHandler(PacketProtocol.PLAY, channel)).isPresent();

        registration.close();

        assertThat(packets.customHandler(PacketProtocol.PLAY, channel)).isEmpty();
        assertThat(messaging.channels()).isEmpty();
    }

    @Test
    void laterServerboundRegistrationBuildsUnderlyingPacketHandler() {
        var packets = new PacketRegistryImpl();
        var messaging = new FandPluginMessaging(packets);
        var channel = Key.key("example:both");

        messaging.register(channel, PluginMessageDirection.CLIENTBOUND);
        assertThat(packets.customHandler(PacketProtocol.PLAY, channel)).isEmpty();

        messaging.register(channel, PluginMessageDirection.SERVERBOUND, noopHandler());

        assertThat(packets.customHandler(PacketProtocol.PLAY, channel)).isPresent();
        assertThat(messaging.channels()).containsExactly(new io.fand.api.messaging.PluginMessageChannel(
                channel,
                PluginMessageDirection.BIDIRECTIONAL));
    }

    @Test
    void removingOneServerboundListenerKeepsSharedChannelActive() {
        var packets = new PacketRegistryImpl();
        var messaging = new FandPluginMessaging(packets);
        var channel = Key.key("example:shared");

        var first = messaging.register(channel, PluginMessageDirection.SERVERBOUND, noopHandler());
        messaging.register(channel, PluginMessageDirection.SERVERBOUND, noopHandler());

        first.close();

        assertThat(packets.customHandler(PacketProtocol.PLAY, channel)).isPresent();
        assertThat(messaging.channels()).containsExactly(new io.fand.api.messaging.PluginMessageChannel(
                channel,
                PluginMessageDirection.SERVERBOUND));
    }

    @Test
    void removingClientboundSideKeepsBidirectionalServerboundHandlerActive() {
        var packets = new PacketRegistryImpl();
        var messaging = new FandPluginMessaging(packets);
        var channel = Key.key("example:both");

        var clientbound = messaging.register(channel, PluginMessageDirection.CLIENTBOUND);
        messaging.register(channel, PluginMessageDirection.SERVERBOUND, noopHandler());

        clientbound.close();

        assertThat(packets.customHandler(PacketProtocol.PLAY, channel)).isPresent();
        assertThat(messaging.channels()).containsExactly(new io.fand.api.messaging.PluginMessageChannel(
                channel,
                PluginMessageDirection.SERVERBOUND));
    }

    @Test
    void removingLastServerboundSideKeepsClientboundChannelVisible() {
        var packets = new PacketRegistryImpl();
        var messaging = new FandPluginMessaging(packets);
        var channel = Key.key("example:both");

        messaging.register(channel, PluginMessageDirection.CLIENTBOUND);
        var serverbound = messaging.register(channel, PluginMessageDirection.SERVERBOUND, noopHandler());

        serverbound.close();

        assertThat(packets.customHandler(PacketProtocol.PLAY, channel)).isEmpty();
        assertThat(messaging.channels()).containsExactly(new io.fand.api.messaging.PluginMessageChannel(
                channel,
                PluginMessageDirection.CLIENTBOUND));
    }

    @Test
    void failingServerboundHandlerDoesNotStopLaterHandlers() {
        var packets = new PacketRegistryImpl();
        var messaging = new FandPluginMessaging(packets);
        var channel = Key.key("example:fanout");
        var called = new AtomicInteger();
        messaging.register(channel, PluginMessageDirection.SERVERBOUND, (player, registeredChannel, payload) -> {
            throw new IllegalStateException("boom");
        });
        messaging.register(channel, PluginMessageDirection.SERVERBOUND, (player, registeredChannel, payload) -> called.incrementAndGet());

        packets.customHandler(PacketProtocol.PLAY, channel)
                .orElseThrow()
                .handle(new StubPacketContext(stubPlayer()), new CustomPacket(channel, new byte[] {1}));

        assertThat(called).hasValue(1);
    }

    @Test
    void serverboundPayloadIsIsolatedBetweenHandlers() {
        var packets = new PacketRegistryImpl();
        var messaging = new FandPluginMessaging(packets);
        var channel = Key.key("example:isolated");
        var secondPayload = new AtomicReference<byte[]>();
        messaging.register(channel, PluginMessageDirection.SERVERBOUND, (player, registeredChannel, payload) -> payload[0] = 9);
        messaging.register(channel, PluginMessageDirection.SERVERBOUND, (player, registeredChannel, payload) ->
                secondPayload.set(Arrays.copyOf(payload, payload.length)));

        packets.customHandler(PacketProtocol.PLAY, channel)
                .orElseThrow()
                .handle(new StubPacketContext(stubPlayer()), new CustomPacket(channel, new byte[] {1, 2, 3}));

        assertThat(secondPayload.get()).containsExactly(1, 2, 3);
    }

    @Test
    void serverboundRegistrationWithoutHandlerIsRejected() {
        var messaging = new FandPluginMessaging(new PacketRegistryImpl());

        assertThatThrownBy(() -> messaging.register(Key.key("example:bad"), PluginMessageDirection.SERVERBOUND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a handler");
    }

    @Test
    void clientboundRegistrationWithHandlerIsRejected() {
        var messaging = new FandPluginMessaging(new PacketRegistryImpl());

        assertThatThrownBy(() -> messaging.register(Key.key("example:bad"), PluginMessageDirection.CLIENTBOUND, noopHandler()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot have a handler");
    }

    @Test
    void pluginChannelConfigurationTaskAdvertisesServerboundChannelsToFabricClients() {
        var messaging = new FandPluginMessaging(new PacketRegistryImpl());
        messaging.register(Key.key("example:client"), PluginMessageDirection.CLIENTBOUND);
        messaging.register(Key.key("jei:delete_player_item"), PluginMessageDirection.SERVERBOUND, noopHandler());
        messaging.register(Key.key("jei:cheat_permission"), PluginMessageDirection.CLIENTBOUND);
        var packets = new ArrayList<Packet<?>>();
        var task = messaging.pluginChannelConfigurationTask();

        task.start(packets::add);

        assertThat(packets).hasSize(2);
        var legacy = payload((ClientboundCustomPayloadPacket) packets.get(0));
        assertThat(legacy.id().toString()).isEqualTo("minecraft:register");
        assertThat(new String(legacy.payload(), java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("c:register\0c:version");
        assertThat(packets.get(1)).isInstanceOf(ClientboundPingPacket.class);

        task.handleCustomPayload(serverboundPayload(
                "minecraft:register",
                "c:register\0c:version".getBytes(java.nio.charset.StandardCharsets.US_ASCII)), packets::add);
        var version = payload((ClientboundCustomPayloadPacket) packets.get(2));
        assertThat(version.id().toString()).isEqualTo("c:version");
        assertThat(new FriendlyByteBuf(Unpooled.wrappedBuffer(version.payload())).readVarIntArray())
                .containsExactly(1);

        task.handleCustomPayload(serverboundPayload("c:version", PluginChannelAdvertiser.commonVersionPayload()), packets::add);
        var register = payload((ClientboundCustomPayloadPacket) packets.get(3));
        assertThat(register.id().toString()).isEqualTo("c:register");
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(register.payload()));
        assertThat(buffer.readVarInt()).isEqualTo(1);
        assertThat(buffer.readUtf()).isEqualTo("play");
        assertThat(buffer.readVarInt()).isEqualTo(1);
        assertThat(buffer.readIdentifier().toString()).isEqualTo("jei:delete_player_item");
        assertThat(buffer.readableBytes()).isZero();

        assertThat(task.tick()).isFalse();
        task.handleCustomPayload(serverboundPayload("c:register", new byte[0]), packets::add);
        assertThat(task.tick()).isTrue();
    }

    @Test
    void pluginChannelConfigurationTaskAdvertisesConfigurationChannelsToFabricClients() {
        var messaging = new FandPluginMessaging(new PacketRegistryImpl());
        messaging.registerInternalConfiguration(
                Key.key("fabric:recipe_sync/supported_serializers"),
                (listener, payload) -> {});
        var packets = new ArrayList<Packet<?>>();
        var task = messaging.pluginChannelConfigurationTask();

        task.start(packets::add);

        assertThat(packets).hasSize(2);
        var legacy = payload((ClientboundCustomPayloadPacket) packets.get(0));
        assertThat(legacy.id().toString()).isEqualTo("minecraft:register");
        assertThat(new String(legacy.payload(), java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("c:register\0c:version\0fabric:recipe_sync/supported_serializers");
    }

    @Test
    void configurationPayloadIsDispatchedToRegisteredHandler() {
        var messaging = new FandPluginMessaging(new PacketRegistryImpl());
        var received = new AtomicReference<byte[]>();
        messaging.registerInternalConfiguration(
                Key.key("fabric:recipe_sync/supported_serializers"),
                (listener, payload) -> received.set(payload));

        boolean handled = messaging.handleConfigurationPayload(
                null,
                net.minecraft.resources.Identifier.parse("fabric:recipe_sync/supported_serializers"),
                new byte[] {1, 2, 3});

        assertThat(handled).isTrue();
        assertThat(received.get()).containsExactly(1, 2, 3);
    }

    @Test
    void configurationPayloadIsIsolatedForEachHandler() {
        var messaging = new FandPluginMessaging(new PacketRegistryImpl());
        var received = new AtomicReference<byte[]>();
        var channel = Key.key("fabric:recipe_sync/supported_serializers");
        messaging.registerInternalConfiguration(channel, (listener, payload) -> payload[0] = 9);
        messaging.registerInternalConfiguration(channel, (listener, payload) -> received.set(payload));

        boolean handled = messaging.handleConfigurationPayload(
                null,
                net.minecraft.resources.Identifier.parse(channel.asString()),
                new byte[] {1, 2, 3});

        assertThat(handled).isTrue();
        assertThat(received.get()).containsExactly(1, 2, 3);
    }

    @Test
    void publicConfigurationHandlerReceivesStableProfileAndChannel() {
        var messaging = new FandPluginMessaging(new PacketRegistryImpl());
        var playerId = UUID.randomUUID();
        var profile = new PlayerProfile(playerId, "CaseSensitive");
        var receivedProfile = new AtomicReference<PlayerProfile>();
        var receivedChannel = new AtomicReference<PluginMessageChannel>();
        var channel = Key.key("tab:bridge-6");
        messaging.registerConfiguration(channel, (receivedPlayer, registeredChannel, payload) -> {
            receivedProfile.set(receivedPlayer);
            receivedChannel.set(registeredChannel);
        });

        boolean handled = messaging.handleConfigurationProfilePayload(
                profile,
                net.minecraft.resources.Identifier.parse(channel.asString()),
                new byte[] {1});

        assertThat(handled).isTrue();
        assertThat(receivedProfile.get().uniqueId()).isEqualTo(playerId);
        assertThat(receivedProfile.get().name()).isEqualTo("CaseSensitive");
        assertThat(receivedChannel.get()).isEqualTo(new PluginMessageChannel(
                channel,
                PluginMessageDirection.SERVERBOUND));
    }

    @Test
    void pluginChannelConfigurationTaskDoesNotBlockVanillaClients() {
        var messaging = new FandPluginMessaging(new PacketRegistryImpl());
        messaging.register(Key.key("jei:delete_player_item"), PluginMessageDirection.SERVERBOUND, noopHandler());
        var packets = new ArrayList<Packet<?>>();
        var task = messaging.pluginChannelConfigurationTask();

        task.start(packets::add);
        var ping = (ClientboundPingPacket) packets.get(1);

        assertThat(task.handlePong(new ServerboundPongPacket(ping.getId()), packets::add)).isTrue();
        assertThat(task.tick()).isTrue();
    }

    @Test
    void pluginChannelConfigurationTaskDoesNotBlockModdedClientsWithoutCommonPackets() {
        var messaging = new FandPluginMessaging(new PacketRegistryImpl());
        messaging.register(Key.key("jei:delete_player_item"), PluginMessageDirection.SERVERBOUND, noopHandler());
        var packets = new ArrayList<Packet<?>>();
        var task = messaging.pluginChannelConfigurationTask();

        task.start(packets::add);
        assertThat(task.handleCustomPayload(serverboundPayload(
                "minecraft:register",
                "example:channel".getBytes(java.nio.charset.StandardCharsets.US_ASCII)), packets::add)).isTrue();

        assertThat(task.tick()).isTrue();
    }

    private static PluginMessageHandler noopHandler() {
        AtomicInteger ignored = new AtomicInteger();
        return (Player player, io.fand.api.messaging.PluginMessageChannel channel, byte[] payload) -> ignored.incrementAndGet();
    }

    private static DiscardedPayload payload(ClientboundCustomPayloadPacket packet) {
        return (DiscardedPayload) packet.payload();
    }

    private static ServerboundCustomPayloadPacket serverboundPayload(String id, byte[] payload) {
        return new ServerboundCustomPayloadPacket(new DiscardedPayload(net.minecraft.resources.Identifier.parse(id), payload));
    }

    private static Player stubPlayer() {
        return (Player) Proxy.newProxyInstance(
                FandPluginMessagingTest.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> "StubPlayer";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class StubPacketContext implements PacketContext {

        private final Player player;

        private StubPacketContext(Player player) {
            this.player = player;
        }

        @Override
        public PacketProtocol protocol() {
            return PacketProtocol.PLAY;
        }

        @Override
        public PacketDirection direction() {
            return PacketDirection.SERVERBOUND;
        }

        @Override
        public Optional<Player> player() {
            return Optional.of(player);
        }

        @Override
        public Optional<io.fand.api.player.PlayerProfile> profile() {
            return Optional.empty();
        }

        @Override
        public Optional<java.net.SocketAddress> remoteAddress() {
            return Optional.empty();
        }
    }
}
