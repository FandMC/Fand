package io.fand.server.network.packet;

import io.fand.api.entity.Player;
import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.CustomPacketHandler;
import io.fand.api.packet.PacketController;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketInterceptor;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.packet.PacketRegistration;
import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import io.fand.server.entity.FandPlayer;
import io.fand.server.entity.PlayerRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link PacketRegistry}, wired into the vanilla
 * {@link Connection} pipeline by patches 0027 (interception) and the
 * {@code DiscardedPayload} byte-retention change.
 *
 * <p><strong>Threading.</strong> {@link #intercept}, {@link #register}, and
 * {@link #send} may be called from any thread and are safe against concurrent
 * dispatch. {@link #interceptInbound} and {@link #interceptOutbound} run on the
 * connection's Netty I/O thread; the interceptors and handlers they invoke run
 * there too and must not block or touch world state directly.
 */
public final class PacketRegistryImpl implements PacketRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketRegistryImpl.class);
    private static final String BUNGEE_NAMESPACE = "bungeecord";
    private static final String VELOCITY_NAMESPACE = "velocity";

    private final PlayerRegistry players;
    private final Map<Class<?>, PacketType> exactTypes;
    private final Map<Class<?>, Optional<PacketType>> resolvedTypes = new ConcurrentHashMap<>();
    private final Map<PacketType, List<PacketInterceptor<?>>> interceptors = new ConcurrentHashMap<>();
    private final Map<String, InboundChannel<?>> inboundChannels = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> outboundChannels = new ConcurrentHashMap<>();
    private final AtomicInteger interceptorCount = new AtomicInteger();

    /** Supplies the registry access used to marshal text/registry-backed packet fields. */
    public static void useRegistries(net.minecraft.core.RegistryAccess access) {
        PacketMarshalling.useRegistries(access);
    }

    public PacketRegistryImpl(PlayerRegistry players) {
        this.players = players;
        var exact = new ConcurrentHashMap<Class<?>, PacketType>();
        PacketTypeMapping.all().forEach((type, cls) -> exact.put(cls, type));
        this.exactTypes = exact;
    }

    @Override
    public <V extends PacketView> PacketRegistration intercept(PacketType type, PacketInterceptor<V> interceptor) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(interceptor, "interceptor");
        interceptors.computeIfAbsent(type, ignored -> new CopyOnWriteArrayList<>()).add(interceptor);
        interceptorCount.incrementAndGet();
        return new SingleRegistration(() -> {
            if (interceptors.getOrDefault(type, List.of()).remove(interceptor)) {
                interceptorCount.decrementAndGet();
            }
        });
    }

    @Override
    public <P extends Record> PacketRegistration register(
            CustomPacketDefinition<P> definition, @Nullable CustomPacketHandler<P> handler) {
        Objects.requireNonNull(definition, "definition");
        String namespace = definition.namespace();
        if (namespace.equals(BUNGEE_NAMESPACE) || namespace.equals(VELOCITY_NAMESPACE)) {
            throw new IllegalArgumentException(
                    "Namespace '" + namespace + "' is reserved for proxy plugin messaging and cannot be claimed");
        }
        PacketDirection direction = definition.direction();
        if ((direction == PacketDirection.INBOUND || direction == PacketDirection.BIDIRECTIONAL) && handler == null) {
            throw new IllegalArgumentException(
                    direction + " definition " + definition.key() + " requires a handler");
        }
        if (direction == PacketDirection.OUTBOUND && handler != null) {
            throw new IllegalArgumentException(
                    "OUTBOUND definition " + definition.key() + " must not have a handler (use BIDIRECTIONAL if you need both)");
        }

        InboundChannel<P> inboundChannel = null;
        if (direction == PacketDirection.INBOUND || direction == PacketDirection.BIDIRECTIONAL) {
            inboundChannel = new InboundChannel<>(definition, handler);
            InboundChannel<?> existing = inboundChannels.putIfAbsent(definition.key(), inboundChannel);
            if (existing != null) {
                throw new IllegalStateException(
                        "Custom packet channel " + definition.key() + " is already registered");
            }
        }
        String outboundKey = null;
        if (direction == PacketDirection.OUTBOUND || direction == PacketDirection.BIDIRECTIONAL) {
            outboundKey = outboundChannels.putIfAbsent(definition.payloadType(), definition.key());
            if (outboundKey != null) {
                if (inboundChannel != null) {
                    inboundChannels.remove(definition.key(), inboundChannel);
                }
                throw new IllegalStateException(
                        "Payload type " + definition.payloadType().getName() + " is already registered to " + outboundKey);
            }
        }

        InboundChannel<P> finalInbound = inboundChannel;
        String finalOutbound = (direction == PacketDirection.OUTBOUND || direction == PacketDirection.BIDIRECTIONAL)
                ? definition.key() : null;
        return new SingleRegistration(() -> {
            if (finalInbound != null) {
                inboundChannels.remove(definition.key(), finalInbound);
            }
            if (finalOutbound != null) {
                outboundChannels.remove(definition.payloadType(), finalOutbound);
            }
        });
    }

    @Override
    public <P extends Record> void send(Player player, P payload) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(payload, "payload");
        String channel = outboundChannels.get(payload.getClass());
        if (channel == null) {
            throw new IllegalArgumentException(
                    "No outbound custom packet definition registered for " + payload.getClass().getName());
        }
        if (!(player instanceof FandPlayer fandPlayer)) {
            throw new IllegalArgumentException("Unsupported player implementation: " + player.getClass().getName());
        }
        byte[] data = RecordPayloadCodec.encode(payload);
        var identifier = identifierOf(channel);
        var packet = new ClientboundCustomPayloadPacket(new DiscardedPayload(identifier, data));
        // Connection.send already marshals onto the channel event loop when called off it.
        fandPlayer.handle().connection.send(packet);
    }

    /**
     * Dispatches an inbound packet. Returns the packet (possibly replaced) to
     * continue processing, or {@code null} to drop it. Runs on the connection
     * I/O thread.
     */
    public @Nullable Packet<?> interceptInbound(Packet<?> packet, Connection connection) {
        if (packet instanceof ServerboundCustomPayloadPacket custom) {
            if (!dispatchInboundCustom(custom.payload(), connection)) {
                return packet;
            }
            return null;
        }
        return runInterceptors(packet, PacketDirection.INBOUND, connection);
    }

    /**
     * Dispatches an outbound packet. Returns the packet (possibly replaced) to
     * continue sending, or {@code null} to drop it. Runs on the connection I/O
     * thread.
     */
    public @Nullable Packet<?> interceptOutbound(Packet<?> packet, Connection connection) {
        return runInterceptors(packet, PacketDirection.OUTBOUND, connection);
    }

    private @Nullable Packet<?> runInterceptors(Packet<?> packet, PacketDirection direction, @Nullable Connection connection) {
        if (interceptorCount.get() == 0) {
            return packet;
        }
        Optional<PacketType> resolved = resolve(packet.getClass());
        if (resolved.isEmpty()) {
            return packet;
        }
        PacketType type = resolved.get();
        if (type.direction() != direction) {
            return packet;
        }
        List<PacketInterceptor<?>> list = interceptors.get(type);
        if (list == null || list.isEmpty()) {
            return packet;
        }

        PacketView view = PacketViewFactory.create(type, packet);
        var controller = new ViewController(view, () -> resolvePlayer(connection));
        for (PacketInterceptor<?> interceptor : list) {
            try {
                invoke(interceptor, controller.view, controller);
            } catch (RuntimeException failure) {
                LOGGER.warn("Packet interceptor for {} failed", type, failure);
            }
            if (controller.cancelled) {
                return null;
            }
        }
        if (controller.replaced) {
            return rebuild(type, packet, controller.view);
        }
        return packet;
    }

    private static @Nullable Packet<?> rebuild(PacketType type, Packet<?> original, PacketView view) {
        DynamicPacketView dynamic = PacketViewFactory.unwrap(view);
        if (dynamic != null) {
            try {
                return dynamic.rebuild();
            } catch (RuntimeException failure) {
                LOGGER.error("Failed to rebuild replaced {}; keeping original", type, failure);
                return original;
            }
        }
        LOGGER.warn("Interceptor replaced {} with an unexpected view {}; keeping original", type, view);
        return original;
    }

    @SuppressWarnings("unchecked")
    private static void invoke(PacketInterceptor<?> interceptor, PacketView view, PacketController controller) {
        ((PacketInterceptor<PacketView>) interceptor).intercept(view, controller);
    }

    /** Returns {@code true} if the payload was consumed by a Fand channel handler. */
    private boolean dispatchInboundCustom(CustomPacketPayload payload, Connection connection) {
        Identifier id = payload.type().id();
        String namespace = id.getNamespace();
        if (namespace.equals(BUNGEE_NAMESPACE) || namespace.equals(VELOCITY_NAMESPACE)) {
            return false;
        }
        InboundChannel<?> channel = inboundChannels.get(namespace + ":" + id.getPath());
        if (channel == null) {
            return false;
        }
        if (!(payload instanceof DiscardedPayload discarded)) {
            return false;
        }
        Player sender = resolvePlayer(connection);
        if (sender == null) {
            LOGGER.warn("Dropping custom packet on {} with no resolvable sender", id);
            return true;
        }
        try {
            channel.deliver(sender, discarded.data());
        } catch (RuntimeException failure) {
            LOGGER.warn("Custom packet handler for {} failed", id, failure);
        }
        return true;
    }

    private @Nullable Player resolvePlayer(@Nullable Connection connection) {
        if (connection == null) {
            return null;
        }
        PacketListener listener = connection.getPacketListener();
        if (listener instanceof ServerGamePacketListenerImpl game) {
            ServerPlayer handle = game.player;
            return players.find(handle.getUUID()).orElse(null);
        }
        return null;
    }

    private Optional<PacketType> resolve(Class<?> packetClass) {
        return resolvedTypes.computeIfAbsent(packetClass, cls -> {
            PacketType exact = exactTypes.get(cls);
            if (exact != null) {
                return Optional.of(exact);
            }
            for (Map.Entry<Class<?>, PacketType> entry : exactTypes.entrySet()) {
                if (entry.getKey().isAssignableFrom(cls)) {
                    return Optional.of(entry.getValue());
                }
            }
            return Optional.empty();
        });
    }

    private static Identifier identifierOf(String channel) {
        int colon = channel.indexOf(':');
        return Identifier.fromNamespaceAndPath(channel.substring(0, colon), channel.substring(colon + 1));
    }

    private record InboundChannel<P extends Record>(CustomPacketDefinition<P> definition, CustomPacketHandler<P> handler) {
        void deliver(Player sender, byte[] data) {
            handler.handle(sender, RecordPayloadCodec.decode(definition.payloadType(), data));
        }
    }

    private static final class ViewController implements PacketController {

        private PacketView view;
        private final java.util.function.Supplier<@Nullable Player> playerResolver;
        private @Nullable Player resolvedPlayer;
        private boolean playerResolved;
        private boolean cancelled;
        private boolean replaced;

        ViewController(PacketView view, java.util.function.Supplier<@Nullable Player> playerResolver) {
            this.view = view;
            this.playerResolver = playerResolver;
        }

        @Override
        public @Nullable Player player() {
            if (!playerResolved) {
                resolvedPlayer = playerResolver.get();
                playerResolved = true;
            }
            return resolvedPlayer;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public void replace(PacketView replacement) {
            if (cancelled) {
                return;
            }
            view = Objects.requireNonNull(replacement, "replacement");
            replaced = true;
        }
    }

    private static final class SingleRegistration implements PacketRegistration {

        private final Runnable onClose;
        private boolean closed;

        SingleRegistration(Runnable onClose) {
            this.onClose = onClose;
        }

        @Override
        public synchronized void close() {
            if (!closed) {
                closed = true;
                onClose.run();
            }
        }
    }
}
