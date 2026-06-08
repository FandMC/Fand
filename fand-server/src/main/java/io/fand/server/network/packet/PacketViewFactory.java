package io.fand.server.network.packet;

import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;

/**
 * Builds the {@link PacketView} an interceptor receives for a packet.
 *
 * <p>For a type with a {@linkplain PacketType#viewType() dedicated typed view},
 * returns a dynamic proxy implementing that interface; its convenience accessors
 * are {@code default} methods, so the handler must invoke them explicitly via
 * {@link InvocationHandler#invokeDefault} — every abstract {@link PacketView}
 * method is delegated to the backing {@link DynamicPacketView} instead. For an
 * untyped packet, returns the {@link DynamicPacketView} directly.
 *
 * <p>Class-based packets (non-record) are snapshotted into an internal record
 * at creation time to enable copy-on-write semantics for {@code replace()}.
 */
final class PacketViewFactory {

    private static final Map<Class<?>, ClassPacketCodec<?, ?>> CLASS_CODECS = new ConcurrentHashMap<>();

    /**
     * Packets that should NOT use {@link GenericClassPacketCodec} because:
     * <ul>
     *   <li>Their constructor parameters don't match field count/order</li>
     *   <li>They have complex initialization logic</li>
     *   <li>They are high-frequency and need a dedicated snapshot for performance</li>
     * </ul>
     */
    private static final Set<Class<?>> GENERIC_CODEC_BLACKLIST = Set.of(
            net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos.class,
            net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot.class,
            net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot.class,
            net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly.class
    );

    static {
        CLASS_CODECS.put(ClientboundSetHealthPacket.class, ClassPacketCodec.setHealth());
        CLASS_CODECS.put(ClientboundContainerSetSlotPacket.class, ClassPacketCodec.containerSetSlot());
    }

    private PacketViewFactory() {
    }

    static PacketView create(PacketType type, Packet<?> packet) {
        // Check if this is a Class packet with a registered snapshot codec
        ClassPacketCodec<?, ?> classCodec = CLASS_CODECS.get(packet.getClass());

        // If no dedicated codec and it's a non-record class, use generic codec (unless blacklisted)
        if (classCodec == null
                && !packet.getClass().isRecord()
                && packet.getClass() != net.minecraft.network.protocol.Packet.class
                && !GENERIC_CODEC_BLACKLIST.contains(packet.getClass())) {
            try {
                classCodec = GenericClassPacketCodec.of((Class<? extends Packet<?>>) packet.getClass());
                // Cache it for reuse
                CLASS_CODECS.put(packet.getClass(), classCodec);
            } catch (IllegalStateException e) {
                // Constructor mismatch or other reflection issue — fall back to record-only view
                // Log once per class, then let it use the fallback path
            }
        }

        if (classCodec != null) {
            // Snapshot the Class packet
            Object snapshot = classCodec.read(packet);
            // For generic codec (Object[] snapshot), build model from the original packet class
            // For dedicated codec (record snapshot), build model from the snapshot record
            Class<?> modelSource = (snapshot instanceof Object[]) ? packet.getClass() : snapshot.getClass();
            PacketFieldModel model = PacketFieldModel.of(modelSource);
            DynamicPacketView core = new DynamicPacketView(type, packet, model, type.viewType(), snapshot, classCodec);
            return wrap(core);
        }

        // Standard flow: record packet, build model from packet directly
        PacketFieldModel model = PacketFieldModel.of(packet.getClass());
        DynamicPacketView core = new DynamicPacketView(type, packet, model, type.viewType());
        return wrap(core);
    }

    /**
     * Wraps a {@link DynamicPacketView} in a typed proxy if its view type is not
     * bare {@link PacketView}. Used by {@link DynamicPacketView#with} to preserve
     * the typed interface across replacement chains.
     */
    static PacketView wrap(DynamicPacketView core) {
        Class<? extends PacketView> viewType = core.viewType();
        if (viewType == PacketView.class) {
            return core;
        }
        return (PacketView) Proxy.newProxyInstance(
                viewType.getClassLoader(), new Class<?>[] {viewType}, new TypedViewHandler(core));
    }

    /**
     * Extracts the backing {@link DynamicPacketView} from a {@link PacketView},
     * which may be either a bare {@code DynamicPacketView} or a typed proxy
     * wrapping one.
     *
     * @return the core dynamic view, or {@code null} if {@code view} is not
     *         a packet view created by this factory
     */
    static @org.jspecify.annotations.Nullable DynamicPacketView unwrap(PacketView view) {
        if (view instanceof DynamicPacketView dynamic) {
            return dynamic;
        }
        if (Proxy.isProxyClass(view.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(view);
            if (handler instanceof TypedViewHandler typed) {
                return typed.core();
            }
        }
        return null;
    }

    private record TypedViewHandler(DynamicPacketView core) implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            return method.invoke(core, args);
        }
    }
}
