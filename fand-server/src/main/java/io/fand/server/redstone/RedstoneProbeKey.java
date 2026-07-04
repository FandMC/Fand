package io.fand.server.redstone;

public record RedstoneProbeKey(RedstoneProbeType type, String level, long blockPos) {
}
