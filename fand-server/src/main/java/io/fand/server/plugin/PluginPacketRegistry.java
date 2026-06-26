package io.fand.server.plugin;

import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.CustomPacketHandler;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketInterceptor;
import io.fand.api.packet.PacketProtocol;
import io.fand.api.packet.PacketRegistration;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import io.fand.api.packet.PlayerInfoPacketFactory;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public final class PluginPacketRegistry implements PacketRegistry {

    private final PacketRegistry delegate;
    private final PluginResourceTracker tracker;

    public PluginPacketRegistry(PacketRegistry delegate, PluginResourceTracker tracker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @Override
    public Collection<PacketType> types() {
        return delegate.types();
    }

    @Override
    public Optional<PacketType> type(PacketProtocol protocol, PacketDirection direction, Key key) {
        return delegate.type(protocol, direction, key);
    }

    @Override
    public PlayerInfoPacketFactory playerInfo() {
        return delegate.playerInfo();
    }

    @Override
    public PacketRegistration intercept(PacketType type, PacketInterceptor<PacketView> interceptor) {
        return tracker.track(delegate.intercept(type, interceptor));
    }

    @Override
    public <T extends PacketView> PacketRegistration intercept(
            PacketType type,
            Class<T> viewType,
            PacketInterceptor<T> interceptor
    ) {
        return tracker.track(delegate.intercept(type, viewType, interceptor));
    }

    @Override
    public PacketRegistration register(CustomPacketDefinition definition) {
        return tracker.track(delegate.register(definition));
    }

    @Override
    public PacketRegistration register(CustomPacketDefinition definition, CustomPacketHandler handler) {
        return tracker.track(delegate.register(definition, handler));
    }
}
