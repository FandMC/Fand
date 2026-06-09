package io.fand.server.network.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketProtocol;
import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import io.fand.api.packet.view.ClientboundAddEntityPacketView;
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

    private interface WrongPacketView extends PacketView {
    }
}
