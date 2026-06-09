package io.fand.api.event.server;

import java.util.Arrays;
import java.util.Objects;

/**
 * PNG server-list icon payload.
 */
public record ServerListIcon(byte[] pngBytes) {

    public ServerListIcon {
        Objects.requireNonNull(pngBytes, "pngBytes");
        pngBytes = Arrays.copyOf(pngBytes, pngBytes.length);
    }

    @Override
    public byte[] pngBytes() {
        return Arrays.copyOf(pngBytes, pngBytes.length);
    }
}
