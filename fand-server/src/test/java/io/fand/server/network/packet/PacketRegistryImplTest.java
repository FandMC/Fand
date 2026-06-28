package io.fand.server.network.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketProtocol;
import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import io.fand.api.packet.view.ClientboundAddEntityPacketView;
import io.fand.api.tablist.TabListEntry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class PacketRegistryImplTest {

    @Test
    void registersAndUnregistersPacketInterceptors() {
        var registry = new PacketRegistryImpl();
        var registration = registry.intercept(
                PacketType.PLAY_CLIENTBOUND_ADD_ENTITY,
                ClientboundAddEntityPacketView.class,
                packet -> {
                });

        assertThat(registration.active()).isTrue();
        assertThat(registry.interceptors(PacketType.PLAY_CLIENTBOUND_ADD_ENTITY)).hasSize(1);

        registration.unregister();

        assertThat(registration.active()).isFalse();
        assertThat(registry.interceptors(PacketType.PLAY_CLIENTBOUND_ADD_ENTITY)).isEmpty();
    }

    @Test
    void rejectsIncompatibleTypedInterceptors() {
        var registry = new PacketRegistryImpl();

        assertThatThrownBy(() -> registry.intercept(
                PacketType.PLAY_CLIENTBOUND_ADD_ENTITY,
                WrongPacketView.class,
                packet -> {
                }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not compatible");
    }

    @Test
    void enforcesCustomChannelDirectionContract() {
        var registry = new PacketRegistryImpl();
        var outbound = CustomPacketDefinition.play(PacketDirection.CLIENTBOUND, Key.key("example:outbound"));
        var inbound = CustomPacketDefinition.play(PacketDirection.SERVERBOUND, Key.key("example:inbound"));

        assertThat(registry.register(outbound).active()).isTrue();
        assertThatThrownBy(() -> registry.register(outbound, (context, packet) -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.register(inbound)).isInstanceOf(IllegalArgumentException.class);
        assertThat(registry.register(inbound, (context, packet) -> {
        }).active()).isTrue();
    }

    @Test
    void rejectsDuplicateCustomChannelsAndRemovesRegistrationPrecisely() {
        var registry = new PacketRegistryImpl();
        var definition = CustomPacketDefinition.play(PacketDirection.SERVERBOUND, Key.key("example:channel"));

        var first = registry.register(definition, (context, packet) -> {
        });
        assertThat(registry.hasCustomChannel(definition)).isTrue();
        assertThatThrownBy(() -> registry.register(definition, (context, packet) -> {
        })).isInstanceOf(IllegalArgumentException.class);

        first.unregister();
        var second = registry.register(definition, (context, packet) -> {
        });
        first.unregister();

        assertThat(second.active()).isTrue();
        assertThat(registry.hasCustomChannel(definition)).isTrue();
    }

    @Test
    void looksUpGeneratedPacketTypes() {
        var registry = new PacketRegistryImpl();

        assertThat(registry.type(PacketProtocol.PLAY, PacketDirection.CLIENTBOUND, Key.key("minecraft:add_entity")))
                .contains(PacketType.PLAY_CLIENTBOUND_ADD_ENTITY);
    }

    @Test
    void exposesPlayerInfoPacketFactory() {
        var registry = new PacketRegistryImpl();
        var entryId = UUID.randomUUID();

        var update = registry.playerInfo().update(List.of(TabListEntry.builder(entryId, "Remote").latency(12).build()));
        var remove = registry.playerInfo().remove(List.of(entryId));

        assertThat(update.packetType()).isEqualTo(PacketType.PLAY_CLIENTBOUND_PLAYER_INFO_UPDATE);
        assertThat(update.value("entries", List.class)).hasSize(1);
        assertThat(remove.packetType()).isEqualTo(PacketType.PLAY_CLIENTBOUND_PLAYER_INFO_REMOVE);
        assertThat(remove.value("profileIds", List.class)).containsExactly(entryId);
    }

    @Test
    void exposesDirectPacketSenderIllusionsAndViewFactory() {
        var registry = new PacketRegistryImpl();

        var view = registry.packet(
                PacketType.PLAY_CLIENTBOUND_BLOCK_CHANGED_ACK,
                Map.of("sequence", 42));

        assertThat(registry.sender()).isNotNull();
        assertThat(registry.illusions()).isNotNull();
        assertThat(view.packetType()).isEqualTo(PacketType.PLAY_CLIENTBOUND_BLOCK_CHANGED_ACK);
        assertThat(view.value("sequence", Integer.class)).isEqualTo(42);
    }

    @Test
    void exposesPacketBuildersAndHelpers() {
        var registry = new PacketRegistryImpl();

        var ack = registry.builder(PacketType.PLAY_CLIENTBOUND_BLOCK_CHANGED_ACK)
                .field("sequence", 42)
                .build();
        var metadata = registry.helpers().entityMetadata(7, List.of("packed"))
                .build();
        var openScreen = registry.helpers().openScreen(3, "menu", "Title").build();

        assertThat(ack.value("sequence", Integer.class)).isEqualTo(42);
        assertThat(metadata.packetType()).isEqualTo(PacketType.PLAY_CLIENTBOUND_SET_ENTITY_DATA);
        assertThat(metadata.value("id", Integer.class)).isEqualTo(7);
        assertThat(metadata.value("packedItems", List.class)).containsExactly("packed");
        assertThat(openScreen.packetType()).isEqualTo(PacketType.PLAY_CLIENTBOUND_OPEN_SCREEN);
        assertThat(openScreen.value("containerId", Integer.class)).isEqualTo(3);
    }

    @Test
    void bridgeRebuildsPacketViewsForDirectSends() {
        var registry = new PacketRegistryImpl();
        var view = registry.packet(
                PacketType.PLAY_CLIENTBOUND_BLOCK_CHANGED_ACK,
                Map.of("sequence", 42));

        var vanilla = registry.vanillaPacket(view);

        assertThat(vanilla).isInstanceOf(net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket.class);
        assertThat(((net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket) vanilla).sequence()).isEqualTo(42);
    }

    private interface WrongPacketView extends PacketView {
    }
}
