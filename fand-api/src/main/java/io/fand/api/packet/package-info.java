/**
 * Packet API: vanilla packet interception and custom packet channels.
 *
 * <p>Interceptors run on the Netty I/O thread of the owning connection, not the
 * main server thread. Touching world or entity state from an interceptor must
 * be marshalled to the server thread by the caller.
 */
@NullMarked
package io.fand.api.packet;

import org.jspecify.annotations.NullMarked;
