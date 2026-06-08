package io.fand.server.plugin;

import io.fand.api.entity.Player;
import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.CustomPacketHandler;
import io.fand.api.packet.PacketController;
import io.fand.api.packet.PacketInterceptor;
import io.fand.api.packet.PacketRegistration;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import org.jspecify.annotations.Nullable;

public final class PluginPacketRegistry implements PacketRegistry {

    private final PacketRegistry delegate;
    private final PluginResourceTracker tracker;

    public PluginPacketRegistry(PacketRegistry delegate, PluginResourceTracker tracker) {
        this.delegate = delegate;
        this.tracker = tracker;
    }

    @Override
    public <V extends PacketView> PacketRegistration intercept(PacketType type, PacketInterceptor<V> interceptor) {
        return tracker.track(delegate.intercept(type, interceptor));
    }

    @Override
    public <P extends Record> PacketRegistration register(
            CustomPacketDefinition<P> definition, @Nullable CustomPacketHandler<P> handler) {
        return tracker.track(delegate.register(definition, handler));
    }

    @Override
    public <P extends Record> void send(Player player, P payload) {
        delegate.send(player, payload);
    }
}
