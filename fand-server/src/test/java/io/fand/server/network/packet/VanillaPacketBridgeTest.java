package io.fand.server.network.packet;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.authlib.GameProfile;
import io.fand.api.packet.CustomPacket;
import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.PacketContext;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketProtocol;
import io.fand.api.packet.PacketType;
import io.fand.api.packet.PlayerInfoEntry;
import io.fand.api.packet.view.ClientboundPlayerInfoUpdatePacketView;
import io.fand.api.packet.view.ClientboundBlockChangedAckPacketView;
import io.fand.api.tablist.TabListEntry;
import io.fand.server.tablist.FandTabListPackets;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.Test;

final class VanillaPacketBridgeTest {

    @Test
    void typedReplacementChainRebuildsRecordPacket() {
        var registry = new PacketRegistryImpl();
        registry.intercept(
                PacketType.PLAY_CLIENTBOUND_BLOCK_CHANGED_ACK,
                ClientboundBlockChangedAckPacketView.class,
                packet -> packet.replace(packet.view().withSequence(2)));
        registry.intercept(
                PacketType.PLAY_CLIENTBOUND_BLOCK_CHANGED_ACK,
                ClientboundBlockChangedAckPacketView.class,
                packet -> {
                    assertThat(packet.view().sequence()).isEqualTo(2);
                    packet.replace(packet.view().withSequence(3));
                });

        var intercepted = registry.interceptOutbound(
                ConnectionProtocol.PLAY,
                PacketFlow.CLIENTBOUND,
                Optional.empty(),
                Optional.empty(),
                null,
                new ClientboundBlockChangedAckPacket(1));

        assertThat(intercepted).isInstanceOf(ClientboundBlockChangedAckPacket.class);
        assertThat(((ClientboundBlockChangedAckPacket) intercepted).sequence()).isEqualTo(3);
    }

    @Test
    void interceptorCanCancelVanillaPacket() {
        var registry = new PacketRegistryImpl();
        registry.intercept(PacketType.PLAY_CLIENTBOUND_BLOCK_CHANGED_ACK, packet -> packet.cancel());

        var intercepted = registry.interceptOutbound(
                ConnectionProtocol.PLAY,
                PacketFlow.CLIENTBOUND,
                Optional.empty(),
                Optional.empty(),
                null,
                new ClientboundBlockChangedAckPacket(1));

        assertThat(intercepted).isNull();
    }

    @Test
    void nonRecordPacketReplacementRebuildsPacket() {
        var registry = new PacketRegistryImpl();
        var original = new ClientboundKeepAlivePacket(1L);
        registry.intercept(PacketType.PLAY_CLIENTBOUND_KEEP_ALIVE, packet -> {
            assertThat(packet.view().value("id", Long.class)).isEqualTo(1L);
            packet.replace(packet.view().with("id", 2L));
        });

        var intercepted = registry.interceptOutbound(
                ConnectionProtocol.PLAY,
                PacketFlow.CLIENTBOUND,
                Optional.empty(),
                Optional.empty(),
                null,
                original);

        assertThat(intercepted).isInstanceOf(ClientboundKeepAlivePacket.class);
        assertThat(intercepted).isNotSameAs(original);
        assertThat(((ClientboundKeepAlivePacket) intercepted).getId()).isEqualTo(2L);
    }

    @Test
    void playerInfoPacketUsesApiEntriesAndPreservesPartialActions() {
        var registry = new PacketRegistryImpl();
        var entryId = UUID.randomUUID();
        var profile = new GameProfile(entryId, "CaseSensitive");
        var original = FandTabListPackets.packet(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY),
                List.of(new ClientboundPlayerInfoUpdatePacket.Entry(
                        entryId,
                        profile,
                        true,
                        42,
                        GameType.DEFAULT_MODE,
                        null,
                        true,
                        0,
                        null)));
        registry.intercept(
                PacketType.PLAY_CLIENTBOUND_PLAYER_INFO_UPDATE,
                ClientboundPlayerInfoUpdatePacketView.class,
                packet -> {
                    assertThat(packet.view().actions()).containsExactly("UPDATE_LATENCY");
                    assertThat(packet.view().entries()).hasSize(1);
                    var entry = packet.view().entries().getFirst();
                    assertThat(entry).isInstanceOf(PlayerInfoEntry.class);
                    assertThat(entry.profile().name()).isEqualTo("CaseSensitive");
                    packet.replace(packet.view().withEntries(List.of(entry.withLatency(99))));
                });

        var intercepted = registry.interceptOutbound(
                ConnectionProtocol.PLAY,
                PacketFlow.CLIENTBOUND,
                Optional.empty(),
                Optional.empty(),
                null,
                original);

        assertThat(intercepted).isInstanceOf(ClientboundPlayerInfoUpdatePacket.class);
        assertThat(intercepted).isNotSameAs(original);
        var info = (ClientboundPlayerInfoUpdatePacket) intercepted;
        assertThat(info.actions()).containsExactly(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY);
        assertThat(info.entries().getFirst().latency()).isEqualTo(99);
        assertThat(info.entries().getFirst().profile()).isSameAs(profile);
    }

    @Test
    void playerInfoFactoryReplacementRebuildsUpdatePacket() {
        var registry = new PacketRegistryImpl();
        var entryId = UUID.randomUUID();
        var original = FandTabListPackets.latency(entryId, 42);
        registry.intercept(PacketType.PLAY_CLIENTBOUND_PLAYER_INFO_UPDATE, packet -> packet.replace(
                registry.playerInfo().update(List.of(TabListEntry.builder(entryId, "Remote")
                        .latency(99)
                        .gameMode(io.fand.api.entity.GameMode.CREATIVE)
                        .order(7)
                        .build()))));

        var intercepted = registry.interceptOutbound(
                ConnectionProtocol.PLAY,
                PacketFlow.CLIENTBOUND,
                Optional.empty(),
                Optional.empty(),
                null,
                original);

        assertThat(intercepted).isInstanceOf(ClientboundPlayerInfoUpdatePacket.class);
        var info = (ClientboundPlayerInfoUpdatePacket) intercepted;
        assertThat(info.actions()).contains(
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER);
        assertThat(info.entries()).hasSize(1);
        var entry = info.entries().getFirst();
        assertThat(entry.profileId()).isEqualTo(entryId);
        assertThat(entry.profile().name()).isEqualTo("Remote");
        assertThat(entry.latency()).isEqualTo(99);
        assertThat(entry.gameMode()).isEqualTo(GameType.CREATIVE);
        assertThat(entry.listOrder()).isEqualTo(7);
    }

    @Test
    void playerInfoFactoryReplacementRebuildsRemovePacket() {
        var registry = new PacketRegistryImpl();
        var oldId = UUID.randomUUID();
        var newId = UUID.randomUUID();
        registry.intercept(PacketType.PLAY_CLIENTBOUND_PLAYER_INFO_REMOVE, packet -> packet.replace(
                registry.playerInfo().remove(List.of(newId))));

        var intercepted = registry.interceptOutbound(
                ConnectionProtocol.PLAY,
                PacketFlow.CLIENTBOUND,
                Optional.empty(),
                Optional.empty(),
                null,
                new ClientboundPlayerInfoRemovePacket(List.of(oldId)));

        assertThat(intercepted).isInstanceOf(ClientboundPlayerInfoRemovePacket.class);
        assertThat(((ClientboundPlayerInfoRemovePacket) intercepted).profileIds()).containsExactly(newId);
    }

    @Test
    void nestedPacketClassResolvesToItsType() {
        var registry = new PacketRegistryImpl();
        var seen = new AtomicReference<PacketContext>();
        registry.intercept(PacketType.PLAY_SERVERBOUND_MOVE_PLAYER_POS, packet -> seen.set(packet.context()));

        var vanillaPacket = new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                1.0, 2.0, 3.0, true, false);
        var intercepted = registry.interceptInbound(
                new StubPacketListener(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND),
                Optional.empty(),
                Optional.empty(),
                null,
                vanillaPacket);

        assertThat(intercepted).isSameAs(vanillaPacket);
        assertThat(seen.get()).isNotNull();
        assertThat(seen.get().direction()).isEqualTo(PacketDirection.SERVERBOUND);
    }

    @Test
    void loginProfileIsExposedBeforePlayerExists() {
        var registry = new PacketRegistryImpl();
        var seen = new AtomicReference<PacketContext>();
        var profile = new io.fand.api.player.PlayerProfile(UUID.randomUUID(), "PreLogin");
        registry.intercept(PacketType.LOGIN_SERVERBOUND_HELLO, packet -> seen.set(packet.context()));

        var vanillaPacket = new net.minecraft.network.protocol.login.ServerboundHelloPacket("PreLogin", profile.uniqueId());
        var intercepted = registry.interceptInbound(
                new StubPacketListener(ConnectionProtocol.LOGIN, PacketFlow.SERVERBOUND),
                Optional.empty(),
                Optional.of(profile),
                null,
                vanillaPacket);

        assertThat(intercepted).isSameAs(vanillaPacket);
        assertThat(seen.get()).isNotNull();
        assertThat(seen.get().player()).isEmpty();
        assertThat(seen.get().profile()).contains(profile);
    }

    @Test
    void registrationStateTracksInterceptorLifecycle() {
        var registry = new PacketRegistryImpl();
        assertThat(registry.hasRegistrations()).isFalse();

        var registration = registry.intercept(PacketType.PLAY_CLIENTBOUND_KEEP_ALIVE, packet -> {});
        assertThat(registry.hasRegistrations()).isTrue();

        registration.unregister();
        assertThat(registry.hasRegistrations()).isFalse();
    }

    @Test
    void registeredCustomChannelReceivesDiscardedPayloadBytes() {
        var registry = new PacketRegistryImpl();
        var channel = Key.key("example:channel");
        var context = new AtomicReference<PacketContext>();
        var packet = new AtomicReference<CustomPacket>();
        registry.register(CustomPacketDefinition.play(PacketDirection.SERVERBOUND, channel), (ctx, payload) -> {
            context.set(ctx);
            packet.set(payload);
        });

        var remoteAddress = new InetSocketAddress("127.0.0.1", 25565);
        var vanillaPacket = new ServerboundCustomPayloadPacket(new DiscardedPayload(
                Identifier.fromNamespaceAndPath("example", "channel"),
                new byte[] {1, 2, 3}));

        var intercepted = registry.interceptInbound(
                new StubPacketListener(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND),
                Optional.empty(),
                Optional.empty(),
                remoteAddress,
                vanillaPacket);

        assertThat(intercepted).isSameAs(vanillaPacket);
        assertThat(context.get().protocol()).isEqualTo(PacketProtocol.PLAY);
        assertThat(context.get().direction()).isEqualTo(PacketDirection.SERVERBOUND);
        assertThat(context.get().remoteAddress()).contains(remoteAddress);
        assertThat(packet.get().channel()).isEqualTo(channel);
        assertThat(packet.get().payload()).containsExactly(1, 2, 3);
    }

    @Test
    void discardedPayloadCodecPreservesBytes() {
        var id = Identifier.fromNamespaceAndPath("example", "raw");
        var codec = DiscardedPayload.<FriendlyByteBuf>codec(id, 16);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buffer, new DiscardedPayload(id, new byte[] {4, 5, 6}));

            var decoded = codec.decode(buffer);

            assertThat(decoded.id()).isEqualTo(id);
            assertThat(decoded.payload()).containsExactly(4, 5, 6);
        } finally {
            buffer.release();
        }
    }

    private record StubPacketListener(ConnectionProtocol protocol, PacketFlow flow) implements PacketListener {

        @Override
        public void onDisconnect(DisconnectionDetails details) {
        }

        @Override
        public boolean isAcceptingMessages() {
            return true;
        }
    }
}
