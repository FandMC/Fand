package io.fand.server.network.packet;

import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.CustomPacketHandler;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketInterceptor;
import io.fand.api.packet.PacketProtocol;
import io.fand.api.packet.PacketRegistration;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import io.fand.api.entity.Player;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.kyori.adventure.key.Key;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jspecify.annotations.Nullable;

public final class PacketRegistryImpl implements PacketRegistry, AutoCloseable {

    private final ConcurrentMap<PacketType, CopyOnWriteArrayList<InterceptorRegistration<? extends PacketView>>> interceptors =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<CustomChannelKey, CustomChannelRegistration> customChannels = new ConcurrentHashMap<>();
    private final VanillaPacketBridge bridge = new VanillaPacketBridge(this);

    @Override
    public Collection<PacketType> types() {
        return List.of(PacketType.values());
    }

    @Override
    public Optional<PacketType> type(PacketProtocol protocol, PacketDirection direction, Key key) {
        return PacketType.find(protocol, direction, key);
    }

    @Override
    public PacketRegistration intercept(PacketType type, PacketInterceptor<PacketView> interceptor) {
        return intercept(type, PacketView.class, interceptor);
    }

    @Override
    public <T extends PacketView> PacketRegistration intercept(
            PacketType type,
            Class<T> viewType,
            PacketInterceptor<T> interceptor
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(viewType, "viewType");
        Objects.requireNonNull(interceptor, "interceptor");
        if (!viewType.isAssignableFrom(type.viewType())) {
            throw new IllegalArgumentException(
                    "View type " + viewType.getName() + " is not compatible with " + type.viewType().getName());
        }
        var registration = new InterceptorRegistration<>(this, type, viewType, interceptor);
        // compute (not computeIfAbsent + add) so the insert is atomic with the
        // empty-list removal in release(); both lock the same map bin.
        interceptors.compute(type, (ignored, registrations) -> {
            var target = registrations == null ? new CopyOnWriteArrayList<InterceptorRegistration<? extends PacketView>>() : registrations;
            target.add(registration);
            return target;
        });
        return registration;
    }

    @Override
    public PacketRegistration register(CustomPacketDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (definition.direction().allowsServerbound()) {
            throw new IllegalArgumentException("Serverbound custom channel " + definition.channel().asString() + " requires a handler");
        }
        return registerCustom(definition, null);
    }

    @Override
    public PacketRegistration register(CustomPacketDefinition definition, CustomPacketHandler handler) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(handler, "handler");
        if (!definition.direction().allowsServerbound()) {
            throw new IllegalArgumentException("Clientbound-only custom channel " + definition.channel().asString() + " cannot have a handler");
        }
        return registerCustom(definition, handler);
    }

    public List<InterceptorRegistration<? extends PacketView>> interceptors(PacketType type) {
        var registrations = interceptors.get(type);
        return registrations == null ? List.of() : registrations;
    }

    /**
     * Cheap pre-check for the per-packet hot path: when no interceptor and no
     * custom channel is registered anywhere, callers skip packet introspection
     * entirely. Interceptor lists are removed from the map when they empty out,
     * so map emptiness tracks registration state.
     */
    public boolean hasRegistrations() {
        return !interceptors.isEmpty() || !customChannels.isEmpty();
    }

    public Optional<CustomPacketHandler> customHandler(PacketProtocol protocol, Key channel) {
        var registration = customChannels.get(new CustomChannelKey(protocol, channel));
        return registration == null ? Optional.empty() : Optional.ofNullable(registration.handler());
    }

    public @Nullable Packet<?> interceptInbound(
            PacketListener listener,
            Optional<? extends Player> player,
            @Nullable SocketAddress remoteAddress,
            Packet<?> packet
    ) {
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packet, "packet");
        return bridge.intercept(listener.protocol(), direction(listener.flow()), player, remoteAddress, packet);
    }

    public @Nullable Packet<?> interceptOutbound(
            ConnectionProtocol protocol,
            PacketFlow flow,
            Optional<? extends Player> player,
            @Nullable SocketAddress remoteAddress,
            Packet<?> packet
    ) {
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(flow, "flow");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packet, "packet");
        return bridge.intercept(protocol, direction(flow), player, remoteAddress, packet);
    }

    boolean hasCustomChannel(CustomPacketDefinition definition) {
        return customChannels.containsKey(CustomChannelKey.from(definition));
    }

    @Override
    public void close() {
        interceptors.clear();
        customChannels.clear();
    }

    private PacketRegistration registerCustom(CustomPacketDefinition definition, CustomPacketHandler handler) {
        var registration = new CustomChannelRegistration(this, definition, handler);
        var existing = customChannels.putIfAbsent(CustomChannelKey.from(definition), registration);
        if (existing != null) {
            throw new IllegalArgumentException(
                    "Custom channel already registered for " + definition.protocol().id() + "/" + definition.channel().asString());
        }
        return registration;
    }

    private void release(InterceptorRegistration<? extends PacketView> registration) {
        interceptors.computeIfPresent(registration.type(), (ignored, registrations) -> {
            registrations.remove(registration);
            return registrations.isEmpty() ? null : registrations;
        });
    }

    private void release(CustomChannelRegistration registration) {
        customChannels.remove(CustomChannelKey.from(registration.definition()), registration);
    }

    private static PacketDirection direction(PacketFlow flow) {
        return flow == PacketFlow.CLIENTBOUND ? PacketDirection.CLIENTBOUND : PacketDirection.SERVERBOUND;
    }

    public static final class InterceptorRegistration<T extends PacketView> implements PacketRegistration {

        private final PacketRegistryImpl owner;
        private final PacketType type;
        private final Class<T> viewType;
        private final PacketInterceptor<T> interceptor;
        private volatile boolean active = true;

        private InterceptorRegistration(
                PacketRegistryImpl owner,
                PacketType type,
                Class<T> viewType,
                PacketInterceptor<T> interceptor
        ) {
            this.owner = owner;
            this.type = type;
            this.viewType = viewType;
            this.interceptor = interceptor;
        }

        public PacketType type() {
            return type;
        }

        public Class<T> viewType() {
            return viewType;
        }

        public PacketInterceptor<T> interceptor() {
            return interceptor;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                owner.release(this);
            }
        }
    }

    private static final class CustomChannelRegistration implements PacketRegistration {

        private final PacketRegistryImpl owner;
        private final CustomPacketDefinition definition;
        private final CustomPacketHandler handler;
        private volatile boolean active = true;

        private CustomChannelRegistration(
                PacketRegistryImpl owner,
                CustomPacketDefinition definition,
                CustomPacketHandler handler
        ) {
            this.owner = owner;
            this.definition = definition;
            this.handler = handler;
        }

        private CustomPacketDefinition definition() {
            return definition;
        }

        private CustomPacketHandler handler() {
            return handler;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                owner.release(this);
            }
        }
    }

    private record CustomChannelKey(PacketProtocol protocol, Key channel) {

        private CustomChannelKey {
            Objects.requireNonNull(protocol, "protocol");
            Objects.requireNonNull(channel, "channel");
        }

        static CustomChannelKey from(CustomPacketDefinition definition) {
            return new CustomChannelKey(definition.protocol(), definition.channel());
        }
    }
}
