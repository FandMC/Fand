package io.fand.server.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.entity.Player;
import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.messaging.PluginMessageHandler;
import io.fand.api.packet.CustomPacket;
import io.fand.api.packet.PacketContext;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketProtocol;
import io.fand.server.network.packet.PacketRegistryImpl;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;
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

    private static PluginMessageHandler noopHandler() {
        AtomicInteger ignored = new AtomicInteger();
        return (Player player, io.fand.api.messaging.PluginMessageChannel channel, byte[] payload) -> ignored.incrementAndGet();
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
        public Optional<java.net.SocketAddress> remoteAddress() {
            return Optional.empty();
        }
    }
}
