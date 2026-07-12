package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketPriority;
import io.fand.api.packet.PacketType;
import io.fand.api.tablist.TabListEntry;
import io.fand.server.network.packet.PacketRegistryImpl;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class PluginPacketRegistryTest {

    @Test
    void pluginPacketRegistrationsAreReleasedWithPluginResources() {
        var registry = new PacketRegistryImpl();
        var resources = new PluginResourceTracker();
        var packets = new PluginPacketRegistry(registry, resources);
        var definition = CustomPacketDefinition.play(PacketDirection.SERVERBOUND, Key.key("example:plugin"));

        packets.register(definition, (context, packet) -> {
        });
        assertThat(registry.customHandler(definition.protocol(), definition.channel())).isPresent();

        resources.close();

        assertThat(registry.customHandler(definition.protocol(), definition.channel())).isEmpty();
    }

    @Test
    void pluginMessageRegistrationsAreReleasedWithPluginResources() {
        var registry = new PacketRegistryImpl();
        var resources = new PluginResourceTracker();
        var messaging = new PluginPluginMessaging(new io.fand.server.messaging.FandPluginMessaging(registry), resources);
        var channel = Key.key("example:plugin_message");

        messaging.register(channel, PluginMessageDirection.SERVERBOUND, (player, registeredChannel, payload) -> {
        });
        assertThat(registry.customHandler(io.fand.api.packet.PacketProtocol.PLAY, channel)).isPresent();

        resources.close();

        assertThat(registry.customHandler(io.fand.api.packet.PacketProtocol.PLAY, channel)).isEmpty();
    }

    @Test
    void configurationMessageRegistrationsAreReleasedWithPluginResources() {
        var registry = new PacketRegistryImpl();
        var resources = new PluginResourceTracker();
        var delegate = new io.fand.server.messaging.FandPluginMessaging(registry);
        var messaging = new PluginPluginMessaging(delegate, resources);
        var channel = Key.key("example:configuration_message");

        messaging.registerConfiguration(channel, (profile, registeredChannel, payload) -> {
        });

        resources.close();

        assertThat(delegate.handleConfigurationPayload(
                null,
                net.minecraft.resources.Identifier.parse(channel.asString()),
                new byte[] {1})).isFalse();
    }

    @Test
    void playerInfoFactoryIsForwardedToPluginRegistry() {
        var registry = new PacketRegistryImpl();
        var resources = new PluginResourceTracker();
        var packets = new PluginPacketRegistry(registry, resources);
        var entryId = UUID.randomUUID();

        var view = packets.playerInfo().update(List.of(TabListEntry.builder(entryId, "Remote").build()));

        assertThat(view.packetType()).isEqualTo(PacketType.PLAY_CLIENTBOUND_PLAYER_INFO_UPDATE);
        assertThat(view.value("entries", List.class)).hasSize(1);
    }

    @Test
    void preservesPriorityAndUnregistersWithPluginResources() {
        var delegate = new PacketRegistryImpl();
        var tracker = new PluginResourceTracker();
        var packets = new PluginPacketRegistry(delegate, tracker);

        var registration = packets.intercept(
                PacketType.PLAY_CLIENTBOUND_KEEP_ALIVE,
                PacketPriority.HIGH,
                packet -> {
                });

        assertThat(delegate.interceptors(PacketType.PLAY_CLIENTBOUND_KEEP_ALIVE))
                .singleElement()
                .extracting(PacketRegistryImpl.InterceptorRegistration::priority)
                .isEqualTo(PacketPriority.HIGH);

        tracker.close();

        assertThat(registration.active()).isFalse();
        assertThat(delegate.interceptors(PacketType.PLAY_CLIENTBOUND_KEEP_ALIVE)).isEmpty();
    }
}
