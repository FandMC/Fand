/**
 * Root package for the Fand plugin API.
 *
 * <p>The API is designed independently from Bukkit and aims to provide a modern,
 * type-safe surface for plugin authors. Implementations live in {@code fand-server}.
 *
 * <p>Unless a method explicitly documents that it is thread-safe or marshals
 * work for the caller, live server state must be accessed from the server
 * thread. Methods returning {@link java.util.concurrent.CompletableFuture}
 * complete when the scheduled server-thread work finishes.
 *
 * <p>Fand uses {@link java.util.Optional} and empty collections for absent
 * values. {@code null} is reserved for members explicitly annotated nullable.
 * Invalid caller input throws {@link IllegalArgumentException}; invalid runtime
 * state, such as reading live world state from the wrong thread, throws
 * {@link IllegalStateException}. Recoverable operation failure is reported with
 * an empty result or {@code false} instead of an exception.
 */
@NullMarked
package io.fand.api;

import org.jspecify.annotations.NullMarked;
