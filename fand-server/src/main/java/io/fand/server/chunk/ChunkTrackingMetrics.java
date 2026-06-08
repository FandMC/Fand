package io.fand.server.chunk;

public record ChunkTrackingMetrics(
        long submittedJobs,
        long completedJobs,
        long appliedJobs,
        long staleJobs,
        long failedJobs,
        long pendingJobs,
        long enteredChunks,
        long leftChunks,
        long totalWorkerNanos
) {
}
