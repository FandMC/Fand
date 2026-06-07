/**
 * Server-side implementation of the Fand packet API: the registry, the
 * stable-enum-to-vanilla-class mapping, view codecs, and custom channel
 * serialization.
 *
 * <p>Dispatch runs on connection Netty I/O threads. See
 * {@link io.fand.server.network.packet.PacketRegistryImpl} for the threading
 * contract of each method.
 */
@NullMarked
package io.fand.server.network.packet;

import org.jspecify.annotations.NullMarked;
