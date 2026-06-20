package io.fand.api.world;

/**
 * Result counters for a completed block scan and its emitted batch changes.
 */
public record BlockScanResult(long scanned, long matched, long changed, long skipped, long failed) {

    public BlockScanResult {
        if (scanned < 0 || matched < 0 || changed < 0 || skipped < 0 || failed < 0) {
            throw new IllegalArgumentException("scan result counters must not be negative");
        }
        if (matched > scanned) {
            throw new IllegalArgumentException("matched cannot exceed scanned");
        }
    }

    public static BlockScanResult empty() {
        return new BlockScanResult(0L, 0L, 0L, 0L, 0L);
    }
}
