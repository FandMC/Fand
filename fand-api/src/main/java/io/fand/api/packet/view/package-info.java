/**
 * Typed, NMS-free views of interceptable vanilla packets.
 *
 * <p>Each record corresponds to exactly one {@link io.fand.api.packet.PacketType}
 * constant and exposes only the fields the project needs. Records are immutable;
 * an interceptor modifies a packet by handing a replacement view to
 * {@link io.fand.api.packet.PacketController#replace}.
 */
@NullMarked
package io.fand.api.packet.view;

import org.jspecify.annotations.NullMarked;
