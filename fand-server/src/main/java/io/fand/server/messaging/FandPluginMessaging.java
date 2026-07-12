package io.fand.server.messaging;

import io.fand.api.entity.Player;
import io.fand.api.messaging.ConfigurationPluginMessageHandler;
import io.fand.api.messaging.PluginMessageChannel;
import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.messaging.PluginMessageHandler;
import io.fand.api.messaging.PluginMessageRegistration;
import io.fand.api.messaging.PluginMessaging;
import io.fand.api.player.PlayerProfile;
import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketRegistration;
import io.fand.server.entity.FandPlayer;
import io.fand.server.network.packet.PacketRegistryImpl;
import io.fand.server.util.ServerThreading;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.kyori.adventure.key.Key;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FandPluginMessaging implements PluginMessaging, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandPluginMessaging.class);

    private final PacketRegistryImpl packets;
    private final PluginChannelAdvertiser advertiser;
    private final ConcurrentMap<Key, ChannelState> channels = new ConcurrentHashMap<>();
    private final ConcurrentMap<Key, CopyOnWriteArrayList<ConfigurationHandlerRegistration>> configurationHandlers =
            new ConcurrentHashMap<>();

    public FandPluginMessaging(PacketRegistryImpl packets) {
        this(packets, java.util.List::of);
    }

    public FandPluginMessaging(PacketRegistryImpl packets, Supplier<Collection<? extends Player>> players) {
        this.packets = Objects.requireNonNull(packets, "packets");
        this.advertiser = new PluginChannelAdvertiser(players);
    }

    @Override
    public Collection<PluginMessageChannel> channels() {
        return channels.entrySet().stream()
                .map(entry -> new PluginMessageChannel(entry.getKey(), entry.getValue().direction()))
                .toList();
    }

    @Override
    public PluginMessageRegistration register(Key channel, PluginMessageDirection direction) {
        if (requiresHandler(direction)) {
            throw new IllegalArgumentException("Serverbound plugin messaging channel " + channel.asString() + " requires a handler");
        }
        return registerInternal(channel, direction, null);
    }

    @Override
    public PluginMessageRegistration register(Key channel, PluginMessageDirection direction, PluginMessageHandler handler) {
        Objects.requireNonNull(handler, "handler");
        if (!requiresHandler(direction)) {
            throw new IllegalArgumentException("Clientbound-only plugin messaging channel " + channel.asString() + " cannot have a handler");
        }
        return registerInternal(channel, direction, handler);
    }

    @Override
    public void send(Player player, Key channel, byte[] payload) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(payload, "payload");
        var state = channels.get(channel);
        if (state == null || !state.direction().allowsClientbound()) {
            throw new IllegalArgumentException("Plugin messaging channel is not registered for clientbound traffic: " + channel.asString());
        }
        if (!(player instanceof FandPlayer fandPlayer)) {
            throw new IllegalArgumentException("Player is not owned by this server: " + player);
        }
        var payloadCopy = Arrays.copyOf(payload, payload.length);
        var handle = fandPlayer.handle();
        var server = handle.level().getServer();
        Runnable send = () -> {
            if (handle.connection != null) {
                handle.connection.send(new ClientboundCustomPayloadPacket(new DiscardedPayload(identifier(channel), payloadCopy)));
            }
        };
        ServerThreading.run(server, send);
    }

    public void advertise(Player player) {
        advertiser.send(player, serverboundChannels());
    }

    public ConfigurationTask pluginChannelConfigurationTask() {
        return new PluginChannelConfigurationTask(serverboundChannels(), configurationServerboundChannels());
    }

    @Override
    public PluginMessageRegistration registerConfiguration(Key channel, ConfigurationPluginMessageHandler handler) {
        Objects.requireNonNull(handler, "handler");
        return registerConfigurationHandler(channel, (listener, profile, payload) -> handler.handle(
                Objects.requireNonNull(profile, "profile"),
                new PluginMessageChannel(channel, PluginMessageDirection.SERVERBOUND),
                payload));
    }

    public ConfigurationMessageRegistration registerInternalConfiguration(
            Key channel,
            ConfigurationMessageHandler handler
    ) {
        Objects.requireNonNull(handler, "handler");
        return registerConfigurationHandler(channel, (listener, profile, payload) -> handler.handle(
                listener,
                payload));
    }

    private ConfigurationMessageRegistration registerConfigurationHandler(
            Key channel,
            ConfigurationDispatchHandler handler
    ) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(handler, "handler");
        var registration = new ConfigurationHandlerRegistration(this, channel, handler);
        configurationHandlers.computeIfAbsent(channel, ignored -> new CopyOnWriteArrayList<>()).add(registration);
        return registration;
    }

    public boolean handleConfigurationPayload(ServerConfigurationPacketListenerImpl listener, Identifier id, byte[] payload) {
        return handleConfigurationPayload(
                listener,
                listener == null ? null : new PlayerProfile(listener.getOwner().id(), listener.getOwner().name()),
                id,
                payload);
    }

    boolean handleConfigurationProfilePayload(PlayerProfile profile, Identifier id, byte[] payload) {
        return handleConfigurationPayload(null, Objects.requireNonNull(profile, "profile"), id, payload);
    }

    private boolean handleConfigurationPayload(
            @Nullable ServerConfigurationPacketListenerImpl listener,
            @Nullable PlayerProfile profile,
            Identifier id,
            byte[] payload
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(payload, "payload");
        var registrations = configurationHandlers.get(key(id));
        if (registrations == null || registrations.isEmpty()) {
            return false;
        }
        boolean handled = false;
        for (var registration : registrations) {
            if (!registration.active()) {
                continue;
            }
            handled = true;
            try {
                registration.handler().handle(listener, profile, Arrays.copyOf(payload, payload.length));
            } catch (RuntimeException failure) {
                LOGGER.warn("Configuration plugin message handler failed for {}", id, failure);
            }
        }
        return handled;
    }

    @Override
    public void close() {
        for (var state : channels.values()) {
            state.close();
        }
        channels.clear();
        configurationHandlers.values().forEach(registrations ->
                registrations.forEach(ConfigurationHandlerRegistration::release));
        configurationHandlers.clear();
    }

    private PluginMessageRegistration registerInternal(
            Key channel,
            PluginMessageDirection direction,
            PluginMessageHandler handler
    ) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(direction, "direction");
        var listener = handler == null ? null : new HandlerRegistration(handler);
        var state = channels.compute(channel, (ignored, existing) -> {
            if (existing == null) {
                return new ChannelState(channel, direction, listener, packets);
            }
            existing.add(direction, listener, packets);
            return existing;
        });
        if (direction.allowsServerbound()) {
            advertiser.broadcast(serverboundChannels());
        }
        return new Registration(this, state, direction, listener);
    }

    private void release(Registration registration) {
        var removedServerbound = new java.util.ArrayList<Key>(1);
        channels.computeIfPresent(registration.state.channel, (ignored, state) -> {
            boolean wasServerbound = state.direction().allowsServerbound();
            state.remove(registration.direction, registration.listener);
            boolean nowServerbound = !state.empty() && state.direction().allowsServerbound();
            if (wasServerbound && !nowServerbound) {
                removedServerbound.add(registration.state.channel);
            }
            return state.empty() ? null : state;
        });
        if (!removedServerbound.isEmpty()) {
            advertiser.broadcastUnregister(removedServerbound);
            advertiser.broadcast(serverboundChannels());
        }
    }

    private static boolean requiresHandler(PluginMessageDirection direction) {
        return direction == PluginMessageDirection.SERVERBOUND || direction == PluginMessageDirection.BIDIRECTIONAL;
    }

    private Collection<Key> serverboundChannels() {
        return channels.entrySet().stream()
                .filter(entry -> entry.getValue().direction().allowsServerbound())
                .map(java.util.Map.Entry::getKey)
                .toList();
    }

    private Collection<Key> configurationServerboundChannels() {
        return configurationHandlers.keySet();
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static Key key(Identifier id) {
        return Key.key(id.getNamespace(), id.getPath());
    }

    private static final class ChannelState {

        private final Key channel;
        private final CopyOnWriteArrayList<HandlerRegistration> handlers = new CopyOnWriteArrayList<>();
        private int clientboundRegistrations;
        private int serverboundRegistrations;
        private PacketRegistration packetRegistration;

        private ChannelState(
                Key channel,
                PluginMessageDirection direction,
                HandlerRegistration handler,
                PacketRegistryImpl packets
        ) {
            this.channel = channel;
            addCounts(direction);
            if (handler != null) {
                handlers.add(handler);
            }
            if (direction.allowsServerbound()) {
                ensurePacketRegistration(packets);
            }
        }

        private PluginMessageDirection direction() {
            if (clientboundRegistrations > 0 && serverboundRegistrations > 0) {
                return PluginMessageDirection.BIDIRECTIONAL;
            }
            return serverboundRegistrations > 0 ? PluginMessageDirection.SERVERBOUND : PluginMessageDirection.CLIENTBOUND;
        }

        private void add(PluginMessageDirection direction, HandlerRegistration handler, PacketRegistryImpl packets) {
            addCounts(direction);
            if (handler != null) {
                handlers.add(handler);
            }
            if (direction.allowsServerbound()) {
                ensurePacketRegistration(packets);
            }
        }

        private void remove(PluginMessageDirection direction, HandlerRegistration handler) {
            removeCounts(direction);
            if (handler != null) {
                handler.release();
                handlers.remove(handler);
            }
            if (serverboundRegistrations == 0 && packetRegistration != null) {
                packetRegistration.unregister();
                packetRegistration = null;
            }
        }

        private boolean empty() {
            return clientboundRegistrations == 0 && serverboundRegistrations == 0;
        }

        private void close() {
            for (var handler : handlers) {
                handler.release();
            }
            handlers.clear();
            clientboundRegistrations = 0;
            serverboundRegistrations = 0;
            if (packetRegistration != null) {
                packetRegistration.unregister();
                packetRegistration = null;
            }
        }

        private void addCounts(PluginMessageDirection direction) {
            if (direction.allowsClientbound()) {
                clientboundRegistrations++;
            }
            if (direction.allowsServerbound()) {
                serverboundRegistrations++;
            }
        }

        private void removeCounts(PluginMessageDirection direction) {
            if (direction.allowsClientbound()) {
                clientboundRegistrations--;
            }
            if (direction.allowsServerbound()) {
                serverboundRegistrations--;
            }
        }

        private void ensurePacketRegistration(PacketRegistryImpl packets) {
            if (packetRegistration != null) {
                return;
            }
            packetRegistration = packets.register(
                    CustomPacketDefinition.play(PacketDirection.SERVERBOUND, channel),
                    (context, packet) -> context.player().ifPresent(player -> {
                        var message = new PluginMessageChannel(channel, direction());
                        byte[] payload = packet.payload();
                        for (var registeredHandler : handlers) {
                            if (registeredHandler.active()) {
                                try {
                                    registeredHandler.handler.handle(
                                            player,
                                            message,
                                            Arrays.copyOf(payload, payload.length));
                                } catch (RuntimeException failure) {
                                    LOGGER.warn("Plugin message handler failed for {}", channel.asString(), failure);
                                }
                            }
                        }
                    }));
        }
    }

    private static final class HandlerRegistration {

        private final PluginMessageHandler handler;
        private volatile boolean active = true;

        private HandlerRegistration(PluginMessageHandler handler) {
            this.handler = handler;
        }

        private boolean active() {
            return active;
        }

        private void release() {
            active = false;
        }
    }

    private static final class Registration implements PluginMessageRegistration {

        private final FandPluginMessaging owner;
        private final ChannelState state;
        private final PluginMessageDirection direction;
        private final HandlerRegistration listener;
        private volatile boolean active = true;

        private Registration(
                FandPluginMessaging owner,
                ChannelState state,
                PluginMessageDirection direction,
                HandlerRegistration listener
        ) {
            this.owner = owner;
            this.state = state;
            this.direction = direction;
            this.listener = listener;
        }

        @Override
        public void close() {
            if (active) {
                active = false;
                owner.release(this);
            }
        }
    }

    @FunctionalInterface
    public interface ConfigurationMessageHandler {

        void handle(ServerConfigurationPacketListenerImpl listener, byte[] payload);
    }

    @FunctionalInterface
    private interface ConfigurationDispatchHandler {

        void handle(
                @Nullable ServerConfigurationPacketListenerImpl listener,
                @Nullable PlayerProfile profile,
                byte[] payload
        );
    }

    public interface ConfigurationMessageRegistration extends PluginMessageRegistration {

        boolean active();

        @Override
        void close();
    }

    private static final class ConfigurationHandlerRegistration implements ConfigurationMessageRegistration {

        private final FandPluginMessaging owner;
        private final Key channel;
        private final ConfigurationDispatchHandler handler;
        private volatile boolean active = true;

        private ConfigurationHandlerRegistration(
                FandPluginMessaging owner,
                Key channel,
                ConfigurationDispatchHandler handler
        ) {
            this.owner = owner;
            this.channel = channel;
            this.handler = handler;
        }

        private ConfigurationDispatchHandler handler() {
            return handler;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void close() {
            if (active) {
                active = false;
                owner.configurationHandlers.computeIfPresent(channel, (ignored, registrations) -> {
                    registrations.remove(this);
                    return registrations.isEmpty() ? null : registrations;
                });
            }
        }

        private void release() {
            active = false;
        }
    }
}
