package io.fand.server.network.packet;

import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.network.protocol.Packet;

/**
 * Builds the {@link PacketView} an interceptor receives for a packet.
 *
 * <p>For a type with a {@linkplain PacketType#viewType() dedicated typed view},
 * returns a dynamic proxy implementing that interface; its convenience accessors
 * are {@code default} methods, so the handler must invoke them explicitly via
 * {@link InvocationHandler#invokeDefault} — every abstract {@link PacketView}
 * method is delegated to the backing {@link DynamicPacketView} instead. For an
 * untyped packet, returns the {@link DynamicPacketView} directly.
 */
final class PacketViewFactory {

    private PacketViewFactory() {
    }

    static PacketView create(PacketType type, Packet<?> packet) {
        PacketFieldModel model = PacketFieldModel.of(packet.getClass());
        DynamicPacketView core = new DynamicPacketView(type, packet, model);
        Class<? extends PacketView> viewType = type.viewType();
        if (viewType == PacketView.class) {
            return core;
        }
        return (PacketView) Proxy.newProxyInstance(
                viewType.getClassLoader(), new Class<?>[] {viewType}, new TypedViewHandler(core));
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
