package io.fand.api.packet;

@FunctionalInterface
public interface PacketInterceptor<T extends PacketView> {

    void intercept(PacketController<T> packet);
}
