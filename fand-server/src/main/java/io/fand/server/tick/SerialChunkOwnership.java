package io.fand.server.tick;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.jspecify.annotations.Nullable;

/**
 * Connects holder lifetimes to the existing serial world thread. Publication uses
 * its event loop, including managed blocking during synchronous chunk requests.
 * It deliberately does not enter a RegionContext: vanilla publication may still
 * synchronously access other cells and the world has not been partitioned yet.
 * The topology is private so worker ticks cannot race this transitional adapter.
 */
public final class SerialChunkOwnership<H> implements AutoCloseable {
    private final Thread controlThread = Thread.currentThread();
    private final Executor executor;
    private final ChunkOwnership<H> ownership;
    private volatile boolean closed;

    public SerialChunkOwnership(Executor executor, int cellSizeChunks, int bufferRadiusCells) {
        this.executor = Objects.requireNonNull(executor, "executor");
        ownership = new ChunkOwnership<>(cellSizeChunks, bufferRadiusCells);
    }

    /** Must complete before the holder is published to the updating chunk map. */
    public Lifetime<H> attach(int chunkX, int chunkZ, H holder) {
        requireControlThread();
        if (closed) {
            throw new IllegalStateException("World chunk ownership is closed");
        }
        var registration = ownership.register(chunkX, chunkZ, holder);
        var pass = ownership.drain(1, 1);
        requireDrained(pass);
        registration.ready().join(); // The private topology has no competing execution leases.
        return new Lifetime<>(this, registration);
    }

    /** Retire only after final unload, never on entry into the pending-unload map. */
    public void detach(Lifetime<H> lifetime) {
        requireControlThread();
        requireLifetime(lifetime);
        if (closed) {
            return;
        }
        var retired = lifetime.registration.retire();
        requireDrained(ownership.drain(1, 1));
        retired.join();
    }

    public Snapshot snapshot() {
        requireControlThread();
        return new Snapshot(ownership.installedChunks(), ownership.topology().activeCells().size(),
                ownership.topology().regions().size(), closed);
    }

    public OwnershipCell cellAtChunk(int chunkX, int chunkZ) {
        return ownership.topology().cellAtChunk(chunkX, chunkZ);
    }

    /** Diagnostic ownership only; this does not grant a region execution lease. */
    public OptionalLong regionIdAt(OwnershipCell cell) {
        requireControlThread();
        var region = ownership.topology().ownerOf(cell).orElse(null);
        return region == null ? OptionalLong.empty() : OptionalLong.of(region.id());
    }

    @Override
    public void close() {
        requireControlThread();
        if (closed) {
            return;
        }
        closed = true;
        var stopped = ownership.shutdown();
        requireDrained(ownership.drain(1, 1));
        stopped.join();
    }

    private void requireControlThread() {
        if (Thread.currentThread() != controlThread) {
            throw new IllegalStateException("Live chunk publication belongs to the world control thread");
        }
    }

    private void requireLifetime(Lifetime<H> lifetime) {
        if (lifetime.owner != this) {
            throw new IllegalArgumentException("Chunk lifetime belongs to another world");
        }
    }

    private static void requireDrained(ChunkOwnership.DrainResult pass) {
        if (pass.queuedRequests() != 0 || pass.pendingChanges() != 0 || pass.busy() != 0) {
            throw new IllegalStateException("Serial chunk ownership did not reach its publication safe point");
        }
    }

    public record Snapshot(int holders, int activeCells, int regions, boolean closed) {}

    public static final class Lifetime<H> {
        private final SerialChunkOwnership<H> owner;
        private final ChunkOwnership.Handle<H> registration;
        private final Set<Publication<?>> pending = ConcurrentHashMap.newKeySet();

        private Lifetime(SerialChunkOwnership<H> owner, ChunkOwnership.Handle<H> registration) {
            this.owner = owner;
            this.registration = registration;
            registration.whenRetired().thenRun(this::cancelPending);
        }

        public boolean active() {
            return !owner.closed && registration.state() == ChunkOwnership.Handle.State.ACTIVE;
        }

        /**
         * Completes prepared data or recovers an IO failure only after validating
         * this exact lifetime on the world thread. Closing ends pending results
         * even if IO never completes or the event loop stops consuming callbacks.
         */
        public <T, R> CompletableFuture<R> publish(
                CompletionStage<T> prepared,
                BiFunction<@Nullable T, @Nullable Throwable, R> install
        ) {
            Objects.requireNonNull(prepared, "prepared");
            Objects.requireNonNull(install, "install");
            var publication = new Publication<R>();
            var delivery = new AtomicReference<BiConsumer<T, Throwable>>();
            delivery.set((data, failure) -> {
                if (publication.result.isDone()) {
                    return;
                }
                try {
                    owner.executor.execute(() -> {
                        if (publication.result.isDone()) {
                            return;
                        }
                        try {
                            owner.requireControlThread();
                            if (!active()) {
                                publication.retire();
                                return;
                            }
                            publication.started = true;
                            publication.result.complete(install.apply(data, failure));
                        } catch (RuntimeException | Error thrown) {
                            publication.result.completeExceptionally(thrown);
                            if (thrown instanceof Error error) {
                                throw error;
                            }
                        }
                    });
                } catch (RuntimeException | Error thrown) {
                    publication.result.completeExceptionally(thrown);
                    if (thrown instanceof Error error) {
                        throw error;
                    }
                }
            });
            pending.add(publication);
            publication.result.whenComplete((value, failure) -> {
                delivery.set(null);
                pending.remove(publication);
            });
            if (!active()) {
                publication.retire();
                return publication.result;
            }
            // The source retains only this detachable callback, not a retired world.
            prepared.whenComplete((data, failure) -> {
                var callback = delivery.getAndSet(null);
                if (callback != null) {
                    callback.accept(data, failure);
                }
            });
            // Retirement may have visited the set before this publication was inserted.
            if (!active()) {
                publication.retire();
            }
            return publication.result;
        }

        private void cancelPending() {
            pending.forEach(Publication::retire);
        }

        private static final class Publication<T> {
            private final CompletableFuture<T> result = new CompletableFuture<>();
            private volatile boolean started;

            private void retire() {
                if (!started) {
                    result.completeExceptionally(new RetiredChunkException());
                }
            }
        }
    }
}
