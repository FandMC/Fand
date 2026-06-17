package io.fand.server.messaging;

import io.fand.api.entity.Player;
import io.fand.api.messaging.PluginMessageChannel;
import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.messaging.PluginMessageHandler;
import io.fand.api.messaging.PluginMessageRegistration;
import io.fand.api.messaging.PluginMessaging;
import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketRegistration;
import io.fand.server.entity.FandPlayer;
import io.fand.server.network.packet.PacketRegistryImpl;
import io.fand.server.util.ServerThreading;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.kyori.adventure.key.Key;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FandPluginMessaging implements PluginMessaging, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandPluginMessaging.class);

    private final PacketRegistryImpl packets;
    private final PluginChannelAdvertiser advertiser;
    private final ConcurrentMap<Key, ChannelState> channels = new ConcurrentHashMap<>();

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

    @Override
    public void close() {
        for (var state : channels.values()) {
            state.close();
        }
        channels.clear();
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
        channels.computeIfPresent(registration.state.channel, (ignored, state) -> {
            state.remove(registration.direction, registration.listener);
            return state.empty() ? null : state;
        });
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

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
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
}
