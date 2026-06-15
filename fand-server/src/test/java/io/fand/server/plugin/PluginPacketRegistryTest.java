package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.PacketDirection;
import io.fand.server.network.packet.PacketRegistryImpl;
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
}
