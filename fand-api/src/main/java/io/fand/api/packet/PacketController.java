package io.fand.api.packet;

/**
 * Mutable control handle passed to packet interceptors.
 */
public interface PacketController<T extends PacketView> {

    PacketContext context();

    PacketType type();

    T view();

    void replace(T replacement);

    boolean replaced();

    void cancel();

    boolean cancelled();
}
