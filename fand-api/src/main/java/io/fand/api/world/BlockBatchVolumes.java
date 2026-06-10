package io.fand.api.world;

final class BlockBatchVolumes {

    static final long TOO_MANY_BLOCKS = (long) Integer.MAX_VALUE + 1L;

    private BlockBatchVolumes() {
    }

    static long cappedVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        long volume = 1L;
        volume = multiplyCapped(volume, span(minX, maxX));
        volume = multiplyCapped(volume, span(minY, maxY));
        volume = multiplyCapped(volume, span(minZ, maxZ));
        return volume;
    }

    private static long multiplyCapped(long volume, long span) {
        if (volume > Integer.MAX_VALUE / span) {
            return TOO_MANY_BLOCKS;
        }
        return volume * span;
    }

    private static long span(int min, int max) {
        return (long) max - min + 1L;
    }
}
